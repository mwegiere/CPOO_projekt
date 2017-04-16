

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class main {
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat img = Imgcodecs.imread("img/sample.jpg", 1);

		// Kamil Kacperski
		WycietySlajd wyciety_slajd = new WycietySlajd(img);
		Mat wyciety_slajd_out = wyciety_slajd.getImg();

		// Kamil Kacperski
		SlajdPerspektywa slajd_perspektywa = new SlajdPerspektywa(wyciety_slajd_out);
		Mat slajd_perspektywa_out = slajd_perspektywa.getImg();

		// Filip Rak
		SlajdBezSzumu slajd_bez_szumu = new SlajdBezSzumu(slajd_perspektywa_out);
		Mat slajd_bez_szumu_out = slajd_bez_szumu.getImg();

		// Maciej Węgierek
		SlajdRownomierneOswietlenie slajd_rownomierne_oswietlenie = new SlajdRownomierneOswietlenie(
				slajd_bez_szumu_out);
		Mat slajd_rownomierne_oswietlenie_out = slajd_rownomierne_oswietlenie.getImg();

		// Marek Ciesielski
		SlajdBrakRozmycia slajd_brak_rozmycia = new SlajdBrakRozmycia(slajd_rownomierne_oswietlenie_out);
		Mat slajd_brak_rozmycia_out = slajd_brak_rozmycia.getImg();

		// Gosia Stawik
		SlajdWykrycieWzorca slajd_wykrycie_wzorca = new SlajdWykrycieWzorca(slajd_brak_rozmycia_out);
		Mat slajd_wykrycie_wzorca_out = slajd_wykrycie_wzorca.getImg();

		Imgcodecs.imwrite("output.png", slajd_wykrycie_wzorca_out);
	}

}
