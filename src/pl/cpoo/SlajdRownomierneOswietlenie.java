package pl.cpoo;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class SlajdRownomierneOswietlenie {

	private Mat InImg;
	private Mat OutImg;

	/*
	 * konstruktor pobierający obraz
	 */
	public SlajdRownomierneOswietlenie(Mat InImg_) {
		InImg = InImg_;
		OutImg = InImg;
	}

	/*
	 * operacja na obrazie
	 */
	private Mat SlajdRownomierneOswietlenieAction() {
		OutImg = InImg; // tu operacja na obrazie
		Imgproc.rectangle(OutImg, new Point(10,10), new Point(50,50), new Scalar(0, 255, 0));
		return OutImg;
	}

	/*
	 * getter poprzez który przekazujemy obraz do kolejnej klasy realizującej
	 * następny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdRownomierneOswietlenieAction();
	}

}
