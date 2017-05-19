package pl.cpoo;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class SlajdPerspektywa {

	private Mat InImg;
	private Mat OutImg;

	/*
	 * konstruktor pobierający obraz
	 */
	public SlajdPerspektywa(Mat InImg_){
		InImg = InImg_;
		OutImg = InImg;
	}

	/*
	 * operacja na obrazie
	 */
	private Mat SlajdPerspektywaAction() {
		OutImg = InImg; // tu operacja na obrazie
		return OutImg;
	}

	/*
	 * getter poprzez który przekazujemy obraz do kolejnej klasy realizującej
	 * następny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdPerspektywaAction();
	}

}
