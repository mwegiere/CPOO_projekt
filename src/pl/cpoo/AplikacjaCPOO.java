package pl.cpoo;
import pl.cpoo.utils.*;
import java.io.File;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class AplikacjaCPOO {
	// wyœwietlanie komunikatów
	public static final boolean LOGS = true;
	
	
	 static Mat poprawaOstrosci(Mat img) 
	{
		//Sprawdzenie poziomu ostrosci, wszystkie slajdy w bazie sa nieostre (te na google). 
		//Dobrze zrobione zdjecie da wiêcej niz 200. Wspomniane zdjecia slajdow to miej niz 50.
		double poziomOstrosci = SlajdBrakRozmycia.GetSharpness(img);
		System.out.println("Poziom Ostrosci: " + poziomOstrosci);
			
			//Próba zastosowania algorytmu poprawiaj¹cego ostroœæ zale¿nie od jakoœci zdjêcia.
			if (poziomOstrosci < 30 && poziomOstrosci >20) img=SlajdBrakRozmycia.deblurFilterLR(img, 2, 6);
			else if (poziomOstrosci <= 20 && poziomOstrosci >10) img=SlajdBrakRozmycia.deblurFilterLR(img, 5, 6);
			else if (poziomOstrosci <= 10) img=SlajdBrakRozmycia.deblurFilterLR(img, 10, 6);
			else if (poziomOstrosci >= 30 && poziomOstrosci <= 80)SlajdBrakRozmycia.HP3(img);
			return img;
		
	}
	
	public static void main(String[] args) {
		String nazwaFolderuWyjsciowego = "output";
		String nazwaFolderuZrodlowego = "img";
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		Mat oryginalneZdjecie;
		File dir = new File(nazwaFolderuWyjsciowego);
		dir.mkdir();
		
		//Wyszukiwanie obrazkow
	    //Vector <String> wektorZdjec = new Vector<String>();
	    LukeFileWalker skanerPlikow=new LukeFileWalker(nazwaFolderuZrodlowego);
		System.out.println(skanerPlikow.get()); 
		WycietySlajd wycietySlajd = new WycietySlajd();
		
		for (String nazwaPliku:skanerPlikow.get())
		{
			oryginalneZdjecie = Imgcodecs.imread(System.getProperty("user.dir") + File.separator + nazwaFolderuZrodlowego + File.separator + nazwaPliku, 1);
			if(LOGS)
			System.out.println("Aktualnie przetwarzamy plik: " + nazwaPliku);
			
			wycietySlajd.wstawZdjecie(oryginalneZdjecie);
			Mat zdjecieWyciete = wycietySlajd.wykonajEdycje(); 
			
			
			Mat zdjeciePoprawionaOstrosc = poprawaOstrosci(zdjecieWyciete);
			
			/*
			// Kamil Kacperski
			WycietySlajd wyciety_slajd = new WycietySlajd(oryginalneZdjecie);
			Mat wyciety_slajd_out = wyciety_slajd.getImg();
		
			// Kamil Kacperski
			SlajdPerspektywa slajd_perspektywa = new SlajdPerspektywa(wyciety_slajd_out);
			Mat slajd_perspektywa_out = slajd_perspektywa.getImg();

			// Filip Rak
			SlajdBezSzumu slajd_bez_szumu = new SlajdBezSzumu(slajd_perspektywa_out);
			Mat slajd_bez_szumu_out = slajd_bez_szumu.getImg();

			// Maciej WÄ™gierek
			SlajdRownomierneOswietlenie slajd_rownomierne_oswietlenie = new SlajdRownomierneOswietlenie(
					slajd_bez_szumu_out);
			Mat slajd_rownomierne_oswietlenie_out = slajd_rownomierne_oswietlenie.getImg();

			// Marek Ciesielski
			SlajdBrakRozmycia slajd_brak_rozmycia = new SlajdBrakRozmycia(slajd_rownomierne_oswietlenie_out);
			Mat slajd_brak_rozmycia_out = slajd_brak_rozmycia.getImg();

			// Gosia Stawik
			SlajdWykrycieWzorca slajd_wykrycie_wzorca = new SlajdWykrycieWzorca(slajd_brak_rozmycia_out);
			Mat slajd_wykrycie_wzorca_out = slajd_wykrycie_wzorca.getImg();

			if(Imgcodecs.imwrite(nazwaWyjsciowa, slajd_wykrycie_wzorca_out))
			*/
			
			//zapis do ktalogu wyjsciowego pod taka sama nazwa
			Imgcodecs.imwrite("output/" + nazwaPliku, zdjeciePoprawionaOstrosc);
			if(LOGS)
			System.out.print("Pomyœlnie zapisano wynik:" + nazwaPliku + " do pliku: " + nazwaFolderuWyjsciowego + "\n");
		}
	}
}
