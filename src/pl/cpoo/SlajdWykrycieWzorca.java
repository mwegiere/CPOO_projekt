package pl.cpoo;

import org.opencv.core.Mat;

public class SlajdWykrycieWzorca {

	private Mat InImg;
	private Mat OutImg;

	/*
	 *  konstruktor pobieraj¹cy obraz
	 */
	public SlajdWykrycieWzorca(Mat InImg_) {
		InImg = InImg_;
		OutImg = InImg;
	}

	/*
	 * operacja na obrazie
	 */
	private Mat SlajdWykrycieWzorcaAction() {
		OutImg = InImg; // tu operacja na obrazie
		return OutImg;
	}

	/*
	 * getter poprzez który przekazujemy obraz do kolejnej klasy realizuj¹cej
	 * nastêpny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdWykrycieWzorcaAction();
	}

}
