package pl.cpoo;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import pl.cpoo.utils.Quadrangle;

public class WycietySlajd {
	
	// wlaczenie modulu do testow rysowania na obrazku
	public static final boolean DRAWINGS = false;
	
	private Mat obrazWejsciowy;
	private Mat obrazTestowy;
	private Mat obrazProgowanyGlobalny;
	private Mat hierarchiaKontur;
	private Mat obrazRoboczy;
	List<MatOfPoint> contours ;
	
	public WycietySlajd(Mat InImg) 
	{
		this.obrazWejsciowy = InImg.clone();
		this.obrazRoboczy = new Mat(InImg.rows(), InImg.cols(), Imgproc.COLOR_RGB2GRAY);
		this.obrazTestowy = new Mat(InImg.rows(), InImg.cols(), Imgproc.COLOR_RGB2GRAY);
		this.obrazProgowanyGlobalny = new Mat(InImg.rows(), InImg.cols(), Imgproc.COLOR_RGB2GRAY);
		this.hierarchiaKontur = new Mat();
		this.contours = new ArrayList<MatOfPoint>();
	}

	public WycietySlajd() 
	{
		this.obrazWejsciowy = null;
		this.obrazTestowy = null;
		this.obrazRoboczy = null;
		this.obrazRoboczy = null;
		this.obrazProgowanyGlobalny = null;
		this.contours  = new ArrayList<MatOfPoint>(); 
	}
	
	
	public Mat wykonajEdycje() 
	{
        MatOfPoint najwiekszyKontur = new MatOfPoint();
        
		// 1.zmiana koloru z RGB na skale szarosci:
		Imgproc.cvtColor(obrazWejsciowy,obrazRoboczy,Imgproc.COLOR_RGB2GRAY);
		
		//2. stosujemy filtr wyg³adzaj¹cy aby zwiêkszyæ efekt podczas wyznaczania krawêdzi
		//Filter size*: Large filters (d > 5) are very slow, so it is recommended to use d=5 for real-time applications,
		//Sigma values*: For simplicity, you can set the 2 sigma values to be the same. If they are small (< 10), the filter will not have much effect, whereas if they are large (> 150), they will have a very strong effect
		Imgproc.bilateralFilter(obrazRoboczy,obrazTestowy,15,150.0,150.0 );
		
		//2.b.Obliczamy wartosæ œredni¹ piksela w obrazku. Poprawne odwo³anie do wyniku: wartoscSrednia.val[0]
		//Scalar wartoscSrednia = Core.mean(obrazRoboczy);
		
		//3. Progowanie obrazu; jak argumentem jest 0 to otsu wyznaczy optymalne ;) wyznaczone optymalne otsu zapisujemy do zmiennej
		double progOtsu = Imgproc.threshold(obrazTestowy, obrazProgowanyGlobalny, 0 , 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU ); 
		
		//4. Do progowania obrazu lepiej jest u¿yæ metody lokalnej, daje o wiele lepsze efekty, ale zabiera wiecej czasu
		Imgproc.adaptiveThreshold(obrazTestowy, obrazRoboczy, 255,
		         Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 61, 4);
		
		//5.Wykrywanie krawedzi, do tego u¿ywamy wyliczony dla ka¿dego obrazka threshold otsu;
		Imgproc.Canny(obrazRoboczy,obrazRoboczy,progOtsu, progOtsu*0.5, 3, false );
		
		//6.stosujemy instrukcje dylacji i  znajdujemy  kontury,
        int dilation_size = 5;
		Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new  Size(2*dilation_size + 1, 2*dilation_size+1));
		Imgproc.dilate(obrazRoboczy,obrazRoboczy,element);
		Imgproc.dilate(obrazRoboczy,obrazRoboczy,element);
		Imgproc.findContours(obrazRoboczy, contours, hierarchiaKontur, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
	    // 7. Wybieramy kontur o najwiêkszym polu
	    double maxArea = 0;
	    Iterator<MatOfPoint> each = contours.iterator();
	    while (each.hasNext()) 
	    {
	        MatOfPoint wrapper = each.next();
	        double area = Imgproc.contourArea(wrapper);
	        if (area > maxArea)
	        {
	        	maxArea = area;
	        	najwiekszyKontur = wrapper;
	        }
	    }

	    
	    // Malujemy kontury jesli chcemy sprawdziæ jak wygl¹daj¹
	    if(DRAWINGS)
	    {
			ArrayList<MatOfPoint> najw  = new ArrayList<MatOfPoint>(); 
	    	najw.add(najwiekszyKontur);
	    	//obrazTestowy = new Mat(obrazWejsciowy.rows(), obrazWejsciowy.cols(), Imgproc.COLOR_RGB2GRAY);
			for (int i = 0; i < najw.size(); i++) 
			{
			     Imgproc.drawContours(obrazTestowy, najw, i, new Scalar(0, 0, 0), -1);
			}
			 najw.clear();
			 contours.clear();
			 return obrazTestowy;
	    }
	    contours.clear();
	 
	    obrazTestowy.release();
		obrazProgowanyGlobalny.release();
		hierarchiaKontur.release();
		obrazRoboczy.release();
	    
	    //Druga czeœæ to znalezienie 4 punktów do wyciêcia prostok¹ta i zmiany jego perspektywy
	    //Tworzymy czworok¹t z najwiêkszego konturu;
	    Quadrangle quadrangle = Quadrangle.fromContour(najwiekszyKontur);
	    //Zmieniamy perspektywe
	    Mat obrazWyjsciowy = quadrangle.warp(obrazWejsciowy);
	    quadrangle.clear();
		return obrazWyjsciowy;
	}

	public void wstawZdjecie(Mat InImg)
	{
		this.obrazWejsciowy = InImg.clone();
		this.obrazTestowy = InImg.clone();
		this.obrazRoboczy = new Mat(InImg.rows(), InImg.cols(), Imgproc.COLOR_RGB2GRAY);
		this.obrazProgowanyGlobalny = new Mat(InImg.rows(), InImg.cols(), Imgproc.COLOR_RGB2GRAY);
		this.hierarchiaKontur = new Mat();
	}
	
}