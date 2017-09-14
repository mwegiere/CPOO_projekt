package pl.cpoo;

import org.opencv.core.Mat;

public class SlajdWykrycieWzorca {

	private Mat InImg;
	private Mat OutImg;

	/*
	 *  konstruktor pobieraj�cy obraz
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
	 * getter poprzez kt�ry przekazujemy obraz do kolejnej klasy realizuj�cej
	 * nast�pny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdWykrycieWzorcaAction();
	}

}
