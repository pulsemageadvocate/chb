package pulad.chb.read.thread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.thymeleaf.util.StringUtils;
import org.unbescape.html.HtmlEscape;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.constant.AboneLevel;
import pulad.chb.constant.ChainIdentifier;
import pulad.chb.dto.AboneBodyDto;
import pulad.chb.dto.AboneIDDto;
import pulad.chb.dto.AboneIPDto;
import pulad.chb.dto.AboneNameDto;
import pulad.chb.dto.AboneWacchoiDto;
import pulad.chb.dto.AbstractAboneDto;
import pulad.chb.dto.NGFileDto;
import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.util.NotShitarabaIdPredicate;
import pulad.chb.util.NumberUtil;
import pulad.chb.util.RegexPredicate;
import pulad.chb.util.SimpleWordPredicate;

public class AboneResProcessor implements ResProcessor {
	private final Pattern regBodyBRPattern = Pattern.compile("(?i) *\\<br\\> *");
	private final Predicate<ResDto> notAnonymousPredicate = x -> !x.isAnonymous();
	private ObjectMapper mapper;

	public AboneResProcessor() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		String bbsDir = bbsObject.getBBSDirectoryName();
		String bbs = bbsObject.getLogDirectoryName();
		String board = bbsObject.getBoardFromThreadUrl(url);
		String threadNGFileName = bbsObject.getThreadFromThreadUrl(url) + ".chb.txt";

		List<Abone> whiteList = new ArrayList<>();
		List<Abone> aboneList = new ArrayList<>();
		// 全体
		readAboneFile(whiteList, aboneList, Config.getRootFolder().resolve("chb.txt").toFile());
		// BBS
		readAboneFile(whiteList, aboneList, Config.getBBSFolder().resolve(bbsDir).resolve("chb.txt").toFile());
		// Board
		readAboneFile(whiteList, aboneList, Config.getLogFolder().resolve(bbs).resolve(board).resolve("chb.txt").toFile());
		// Thread
		readAboneFile(whiteList, aboneList, Config.getLogFolder().resolve(bbs).resolve(board).resolve(threadNGFileName).toFile());

		HashSet<Integer> aboneIndex = new HashSet<>();
		for (ResDto dto : res.values()) {
			// ホワイトリスト
			// addするのでintで回す
			for (int i = 0; i < whiteList.size(); i++) {
				Abone abone = whiteList.get(i);

				// 期限切れチェック
				long create = abone.aboneDto.getCreateDate();
				long durationDay = (long) abone.aboneDto.getDurationDay();
				if (durationDay > 0) {
					long from = create - durationDay * 86400000L;
					long to = create + durationDay * 86400000L;
					if (dto.getTimeLong() < from || to <= dto.getTimeLong()) {
						continue;
					}
				}

				if (abone.predicate.test(dto)) {
					dto.setAbone(AboneLevel.WHITE);
					dto.setAboneLabel(abone.label);
					break;
				}
			}
			if (dto.getAbone() == AboneLevel.WHITE) {
				continue;
			}

			// あぼ～ん
			// addするのでintで回す
			for (int i = 0; i < aboneList.size(); i++) {
				Abone abone = aboneList.get(i);

				// 期限切れチェック
				long create = abone.aboneDto.getCreateDate();
				long durationDay = (long) abone.aboneDto.getDurationDay();
				if (durationDay > 0) {
					long from = create - durationDay * 86400000L;
					long to = create + durationDay * 86400000L;
					if (dto.getTimeLong() < from || to <= dto.getTimeLong()) {
						continue;
					}
				}

				if (abone.predicate.test(dto)) {
					// あぼ～んレベルが前のあぼ～ん以下の場合は適用しない
					AboneLevel level = abone.aboneDto.isInvisible() ? AboneLevel.INVISIBLE : AboneLevel.ABONE;
					AboneLevel oldLevel = dto.getAbone();
					if (level.compareTo(oldLevel) > 0) {
						dto.setAbone(level);
						// TODO:元を特定する方法
						dto.setAboneSource(0);
					}
					if (oldLevel == AboneLevel.NONE) {
						// 新規あぼ～んなので原因表示
						dto.setAboneLabel(abone.label);
					} else if (abone.source <= 0) {
						// あぼ～ん済みかつ連鎖あぼ～んではないので原因追記
						dto.setAboneLabel(dto.getAboneLabel() + "＋" + abone.label);
					}
					// あぼ～ん済みかつ連鎖あぼ～んの場合は追記しない

					// 連鎖ID
					Abone chainAbone = new Abone();
					switch (abone.aboneDto.getChainIdentifier()) {
					case ID:
					{
						if (!StringUtils.isEmpty(dto.getId())) {
							AboneIDDto a = new AboneIDDto();
							a.setWord(dto.getId());
							a.setRegex(false);
							a.setInvisible(abone.aboneDto.isInvisible());
							a.setLabel("連鎖[ID]：" + abone.aboneDto.getLabel());
							a.setChainIdentifier(ChainIdentifier.NONE);
							a.setReferenceChain(0);
							chainAbone.aboneDto = a;
							chainAbone.predicate = new NotShitarabaIdPredicate().and(new SimpleWordPredicate<ResDto>(dto.getId(), x -> normalize(x.getId())));
							chainAbone.label = "連鎖[ID]：" + abone.label;
							chainAbone.source = dto.getNumber();
							aboneList.add(chainAbone);
						}
						break;
					}
					case WACCHOI:
					{
						if (!StringUtils.isEmpty(dto.getWacchoi())) {
							AboneWacchoiDto a = new AboneWacchoiDto();
							a.setWord(dto.getWacchoi());
							a.setRegex(false);
							a.setInvisible(abone.aboneDto.isInvisible());
							a.setLabel("連鎖[ﾜｯﾁｮｲ]：" + abone.aboneDto.getLabel());
							a.setChainIdentifier(ChainIdentifier.NONE);
							a.setReferenceChain(0);
							chainAbone.aboneDto = a;
							chainAbone.predicate = new SimpleWordPredicate<ResDto>(dto.getWacchoi(), x -> normalize(x.getWacchoi()));
							chainAbone.label = "連鎖[ﾜｯﾁｮｲ]：" + abone.label;
							chainAbone.source = dto.getNumber();
							aboneList.add(chainAbone);
						}
						break;
					}
					case WACCHOI_LOWER:
					{
						if (!StringUtils.isEmpty(dto.getWacchoiLower())) {
							AboneWacchoiDto a = new AboneWacchoiDto();
							a.setWord(dto.getWacchoiLower());
							a.setRegex(false);
							a.setInvisible(abone.aboneDto.isInvisible());
							a.setLabel("連鎖[ﾜｯﾁｮｲ下4桁]：" + abone.aboneDto.getLabel());
							a.setChainIdentifier(ChainIdentifier.NONE);
							a.setReferenceChain(0);
							chainAbone.aboneDto = a;
							chainAbone.predicate = new SimpleWordPredicate<ResDto>(dto.getWacchoiLower(), x -> normalize(x.getWacchoiLower()));
							chainAbone.label = "連鎖[ﾜｯﾁｮｲ下4桁]：" + abone.label;
							chainAbone.source = dto.getNumber();
							aboneList.add(chainAbone);
						}
						break;
					}
					case IP:
					{
						if (!StringUtils.isEmpty(dto.getIp())) {
							AboneIPDto a = new AboneIPDto();
							a.setWord(dto.getIp());
							a.setRegex(false);
							a.setInvisible(abone.aboneDto.isInvisible());
							a.setLabel("連鎖[IP]：" + abone.aboneDto.getLabel());
							a.setChainIdentifier(ChainIdentifier.NONE);
							a.setReferenceChain(0);
							chainAbone.aboneDto = a;
							chainAbone.predicate = new SimpleWordPredicate<ResDto>(dto.getIp(), x -> normalize(x.getIp()));
							chainAbone.label = "連鎖[IP]：" + abone.label;
							chainAbone.source = dto.getNumber();
							aboneList.add(chainAbone);
						}
						break;
					}
					default:
						break;
					}

					// 参照連鎖
					if (abone.aboneDto.getReferenceChain() > 0) {
						aboneIndex.add(NumberUtil.integerCache(dto.getNumber()));
					}
				}
			}
			// 参照連鎖
			Set<Integer> referSet = dto.getReferSet();
			if (referSet != null && AboneLevel.NONE.compareTo(dto.getAbone()) <= 0) {
				for (Integer refer : referSet) {
					if (aboneIndex.contains(refer)) {
						ResDto aboneRes = res.get(refer);
						dto.setAbone(aboneRes.getAbone());
						dto.setAboneSource(refer);
						dto.setAboneLabel("連鎖[参照]：" + aboneRes.getAboneLabel());
					}
				}
			}
		}
	}

	private void readAboneFile(List<Abone> whiteList, List<Abone> aboneList, File file) {
		try {
			if (!file.exists()) {
				return;
			}
			if (file.length() == 0) {
				file.delete();
				return;
			}
			NGFileDto ngFileDto = mapper.readValue(file, NGFileDto.class);
			if (ngFileDto == null) {
				return;
			}
			if (ngFileDto.getName() != null) {
				for (AboneNameDto name : ngFileDto.getName()) {
					Abone abone = new Abone();
					abone.aboneDto = name;
					abone.predicate = notAnonymousPredicate.and(
							name.isRegex() ?
									new RegexPredicate<ResDto>(name.getWord(), x -> normalize(x.getName())) :
										new SimpleWordPredicate<ResDto>(name.getWord(), x -> normalize(x.getName())));
					abone.label = "名前[" + name.getLabel() + "]";
					if (name.isWhite()) {
						whiteList.add(abone);
					} else {
						aboneList.add(abone);
					}
				}
			}
			if (ngFileDto.getWacchoi() != null) {
				for (AboneWacchoiDto wacchoi : ngFileDto.getWacchoi()) {
					Abone abone = new Abone();
					abone.aboneDto = wacchoi;
					abone.predicate = wacchoi.isRegex() ?
							new RegexPredicate<ResDto>(wacchoi.getWord(), x -> normalize(x.getWacchoi())) :
								new SimpleWordPredicate<ResDto>(wacchoi.getWord(), x -> normalize(x.getWacchoi()));
					abone.label = "ﾜｯﾁｮｲ[" + wacchoi.getLabel() + "]";
					if (wacchoi.isWhite()) {
						whiteList.add(abone);
					} else {
						aboneList.add(abone);
					}
				}
			}
			if (ngFileDto.getIp() != null) {
				for (AboneIPDto ip : ngFileDto.getIp()) {
					Abone abone = new Abone();
					abone.aboneDto = ip;
					abone.predicate = ip.isRegex() ?
							new RegexPredicate<ResDto>(ip.getWord(), x -> normalize(x.getIp())) :
								new SimpleWordPredicate<ResDto>(ip.getWord(), x -> normalize(x.getIp()));
					abone.label = "IP[" + ip.getLabel() + "]";
					if (ip.isWhite()) {
						whiteList.add(abone);
					} else {
						aboneList.add(abone);
					}
				}
			}
			if (ngFileDto.getId() != null) {
				for (AboneIDDto id : ngFileDto.getId()) {
					Abone abone = new Abone();
					abone.aboneDto = id;
					abone.predicate = id.isRegex() ?
							new RegexPredicate<ResDto>(id.getWord(), x -> normalize(x.getId())) :
								new SimpleWordPredicate<ResDto>(id.getWord(), x -> normalize(x.getId()));
					abone.label = "ID[" + id.getLabel() + "]";
					if (id.isWhite()) {
						whiteList.add(abone);
					} else {
						aboneList.add(abone);
					}
				}
			}
			if (ngFileDto.getBody() != null) {
				for (AboneBodyDto body : ngFileDto.getBody()) {
					Abone abone = new Abone();
					abone.aboneDto = body;
					abone.predicate = body.isRegex() ?
							new RegexPredicate<ResDto>(body.getWord(), x -> normalize(x.getBody())) :
								new SimpleWordPredicate<ResDto>(body.getWord(), x -> normalize(x.getBody()));
					abone.label = "本文[" + body.getLabel() + "]";
					if (body.isWhite()) {
						whiteList.add(abone);
					} else {
						aboneList.add(abone);
					}
				}
			}
		} catch (IOException e) {
			App.logger.error("readAboneFile失敗", e);
			throw new RuntimeException("readAboneFile失敗", e);
		}
	}

	/**
	 * 本文そのままではスペースや<br>が邪魔なので正規化する。
	 * 名前欄の数値文字参照も解除する。
	 * @param source
	 * @return
	 */
	private String normalize(String source) {
		if (source == null) {
			return null;
		}
		// 数値文字参照
		String s = HtmlEscape.unescapeHtml(source);
		// 最初と最後のスペースを削除
		s = s.trim();
		// 改行前後のスペースを削除、<br>を\nに置換
		s = regBodyBRPattern.matcher(s).replaceAll("\n");
		return s;
	}

	private static class Abone {
		AbstractAboneDto aboneDto;
		Predicate<ResDto> predicate;
		String label;
		int source = 0;
	}
}
