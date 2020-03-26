package pulad.chb.launcher;

public class DebugLauncher extends Launcher {

	public static void main(String[] args){
		addJarDir("R:\\V2C\\chb_lib");
		addClassDir("..\\chb_base\\target\\classes");
		addClassDir("..\\chb\\target\\classes");
		addClassDir("..\\chb_ch\\target\\classes");
		addClassDir("..\\chb_shitaraba\\target\\classes");

		run(args);
	}
}
