import pulad.chb.interfaces.BBS;
import pulad.chb.shitaraba.Shitaraba;

module pulad.chb.ch {
	requires transitive slf4j.api;
	requires transitive pulad.chb.base;
	provides BBS with Shitaraba;
}