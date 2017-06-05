package pl.cpoo;
import org.opencv.core.Mat;

public class SlajdBezSzumu {

	private Mat InImg;
	private Mat OutImg;

	/*
	 * konstruktor pobierający obraz
	 */
	public SlajdBezSzumu(Mat InImg_) {
		InImg = InImg_;
		OutImg = InImg;
	}

	/*
	 * operacja na obrazie
	 */
	private Mat SlajdBezSzumuAction() {
		OutImg = InImg; // tu operacja na obrazie
		return OutImg;
	}

	/*
	 * getter poprzez który przekazujemy obraz do kolejnej klasy realizującej
	 * następny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdBezSzumuAction();
	}

}
