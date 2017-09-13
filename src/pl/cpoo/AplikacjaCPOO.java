package pl.cpoo;

import pl.cpoo.utils.*;
import java.io.File;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class AplikacjaCPOO {
	// wy�wietlanie komunikat�w
	public static final boolean LOGS = true;

	static Mat poprawaOstrosci(Mat img) {
		// Sprawdzenie poziomu ostrosci, wszystkie slajdy w bazie sa nieostre
		// (te na google).
		// Dobrze zrobione zdjecie da wi�cej niz 200. Wspomniane zdjecia slajdow
		// to miej niz 50.
		double poziomOstrosci = SlajdBrakRozmycia.GetSharpness(img);
		System.out.println("Poziom Ostrosci: " + poziomOstrosci);

		// Pr�ba zastosowania algorytmu poprawiaj�cego ostro�� zale�nie od
		// jako�ci zdj�cia.
		if (poziomOstrosci < 30 && poziomOstrosci > 20)
			img = SlajdBrakRozmycia.deblurFilterLR(img, 2, 6);
		else if (poziomOstrosci <= 20 && poziomOstrosci > 10)
			img = SlajdBrakRozmycia.deblurFilterLR(img, 5, 6);
		else if (poziomOstrosci <= 10)
			img = SlajdBrakRozmycia.deblurFilterLR(img, 10, 6);
		else if (poziomOstrosci >= 30 && poziomOstrosci <= 80)
			SlajdBrakRozmycia.HP3(img);
		return img;

	}

	public static void main(String[] args) {
		String nazwaFolderuWyjsciowego = "output";
		String nazwaFolderuZrodlowego = "img";
		String nazwaFolderuMaski = "mask";
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		Mat oryginalneZdjecie;
		Mat maska;
		File dir = new File(nazwaFolderuWyjsciowego);
		dir.mkdir();

		// Wyszukiwanie obrazkow
		// Vector <String> wektorZdjec = new Vector<String>();
		LukeFileWalker skanerPlikow = new LukeFileWalker(nazwaFolderuZrodlowego);
		System.out.println(skanerPlikow.get());

		maska = Imgcodecs.imread(System.getProperty("user.dir") + File.separator
				+ nazwaFolderuMaski + File.separator + "maska.JPG", 1);
		
		for (String nazwaPliku : skanerPlikow.get()) {
			oryginalneZdjecie = Imgcodecs.imread(System.getProperty("user.dir") + File.separator
					+ nazwaFolderuZrodlowego + File.separator + nazwaPliku, 1);
			if (LOGS)
				System.out.println("Aktualnie przetwarzamy plik: " + nazwaPliku);
			
			// Marek Ciesielski
			/*****/
			Mat zdjeciePoprawionaOstrosc;
			if (Config.wlacz_slajdBrakRozmycia) {
				if(LOGS)
					System.out.println("---->Wyostrzanie: " + nazwaPliku);
				zdjeciePoprawionaOstrosc = poprawaOstrosci(oryginalneZdjecie);
			} else {
				zdjeciePoprawionaOstrosc = oryginalneZdjecie;
			}
		
			// Filip Rak
			/*****/
			Mat slajd_bez_szumu;
			if (Config.wlacz_slajdBezSzumu) {
				if (LOGS)
					System.out.println("---->Usuwanie szumu: " + nazwaPliku);
				slajd_bez_szumu = new SlajdBezSzumu(zdjeciePoprawionaOstrosc).getImg();
			} else {
				slajd_bez_szumu = zdjeciePoprawionaOstrosc;
			}
			
			// Kamil Kacperski
			/*****/
			Mat zdjecieWyciete;
			if (Config.wlacz_wycietySlajd) {
				if (LOGS)
					System.out.println("---->Wycinanie slajdu: " + nazwaPliku);
				WycietySlajd wycietySlajd = new WycietySlajd();
				wycietySlajd.wstawZdjecie(slajd_bez_szumu);
				zdjecieWyciete = wycietySlajd.wykonajEdycje();
			} else {
				zdjecieWyciete = slajd_bez_szumu;
			}

			// Maciej Węgierek
			/*****/
			Mat slajd_rownomierne_oswietlenie_out;
			if (Config.wlacz_slajdRownOswietlenie) {
				if (LOGS)
					System.out.println("---->Rownomierne oswietlenie: " + nazwaPliku);
				SlajdRownomierneOswietlenie slajd_rownomierne_oswietlenie = new SlajdRownomierneOswietlenie(
						zdjeciePoprawionaOstrosc, maska);
				slajd_rownomierne_oswietlenie_out = slajd_rownomierne_oswietlenie.getImg();
			} else {
				slajd_rownomierne_oswietlenie_out = zdjecieWyciete;
			}
			
			// Gosia Stawik
			/*****/
			Mat slajd_wykrycie_wzorca_out;
			if (Config.wlacz_slajdWykrycieWzorca) {
				if (LOGS)
					System.out.println("---->Wykrywanie wzorca: " + nazwaPliku);
				SlajdWykrycieWzorca slajd_wykrycie_wzorca = new SlajdWykrycieWzorca(
						slajd_rownomierne_oswietlenie_out);
				slajd_wykrycie_wzorca_out = slajd_wykrycie_wzorca.getImg();
			} else {
				slajd_wykrycie_wzorca_out = slajd_rownomierne_oswietlenie_out;
			}
			
			//zapis do ktalogu wyjsciowego pod taka sama nazwa
			Imgcodecs.imwrite("output/" + nazwaPliku, slajd_wykrycie_wzorca_out);
			if(LOGS)
				System.out.print("\nOK! Pomy�lnie zapisano wynik:" + nazwaPliku + " do pliku: " + nazwaFolderuWyjsciowego + "\n");
		}
	}
}
