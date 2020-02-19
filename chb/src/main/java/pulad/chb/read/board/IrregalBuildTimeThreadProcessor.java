package pulad.chb.read.board;

import java.time.LocalDateTime;
import java.util.List;

import pulad.chb.constant.AboneLevel;
import pulad.chb.dto.ThreadDto;

/**
 * 「★ 5ちゃんねるへようこそ」等を非表示
 * @author pulad
 *
 */
public class IrregalBuildTimeThreadProcessor implements ThreadProcessor {

	@Override
	public void process(List<ThreadDto> list) {
		for (ThreadDto dto : list) {
			LocalDateTime buildTime = dto.getBuildTime();
			if (buildTime != null && buildTime.getYear() >= 2100) {
				dto.setAbone(AboneLevel.INVISIBLE);
			}
		}
	}

}
