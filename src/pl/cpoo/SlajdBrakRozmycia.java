package pl.cpoo;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

/**
 * Klasa gwarantuje funkcjonalnoœæ:
 * - Filtr górnoprzepustowy o odpowiedzi impulsowej {-1, -1, -1, -1, 9, -1, -1, -1, -1};
 * - Sprawdzenie poziomu ostroœci metod¹ Laplasjanu
 * - Poprawa ostroœci algorytmem LR 
 * @author Kot
 *
 */

public class SlajdBrakRozmycia {
	
	
	
	/**
	 * Stala zawierajaca odpowiedŸ impulsow¹ filtru High Pass 
	 */
	private static final float[] impulseResponseHP = {-1, -1, -1, -1, 9, -1, -1, -1, -1};
	/**
	 * Stala zawierajaca kernel filrtu LR
	 */
	private static final Mat kernelF2D = new Mat(3, 3, CvType.CV_32FC1);
	/**
	 * +epsilon
	 */
	private static final double EPSILON=2.2204e-16;
	
	/**
	 * Proszê o nie tworzenie obiektów tej klasy
	 */
	private SlajdBrakRozmycia() {
		
	}

	/**
	 * metoda filtru gornoprzepustowego
	 * @param image wskaznik na przetwarzany obraz
	 */
	public static void HP3(Mat image) {
		kernelF2D.put(0, 0, impulseResponseHP);
		Imgproc.filter2D(image, image, -1, kernelF2D);
	}
	
	/**
	 * Detekcja ostrosci.
	 * OpenCV port of 'LAPV' algorithm (Pech2000)
	 * Chodzi o to ¿eby dopasowaæ iloœæ iteracji algorytmu LR do faktycznej ostroœci obrazu
	 * Wspó³czynnik ostroœci wiêkszy od 80 oznacza, ¿e obraz jest ca³kiem ostry, Mniej => rozmyty
	 * @param img Przetwarzany obraz
	 * @return Poziom ostroœci zwykle w przedziale du¿ym - Proponuje ustawiæ próg na 20 (mniejsze do poprawy).
	 */
	public static double GetSharpness( Mat img)
	{	
		Mat in = img.clone();
	    Mat out = new Mat(img.size(), CvType.CV_64F);
	    

	    try{
	    	Imgproc.Laplacian(in, out, img.depth());
	    }
		catch(Exception e)
		{
			System.out.println("Wyj?tek funkcji Laplacian:  SlajdBrakRozmycia " + e.toString());
		}
	    
	    
	   
	    MatOfDouble mu = new MatOfDouble();
	    MatOfDouble sigma = new MatOfDouble();
	    Core.meanStdDev(out, mu, sigma);
	    double maxLap = sigma.get(0,0)[0]*sigma.get(0,0)[0];
	    
		in.release();
		out.release();
		mu.release();
		sigma.release();
		System.gc();
	    return maxLap;
	}
	/**
	 * Metoda poprawiaj¹ca ostroœæ algorytmem LR. Jest on bardzo skuteczny ale dzia³a raczej powoli (Przynajmniej w Javie).
	 * Lepiej najpierw zbadaæ ostroœæ metod¹ GetSharpness() i sprawdziæ czy nie wystarczy np. u¿yæ filtru górnoprzepustowego.
	 * 
	 * @param img przetwarzany obrazek wejœcie
	 * @param num_iterations iloœæ iteracji dla bardziej rozmytych ustawiæ wiêcej (testowane w przedziale 2-50)
	 * @param sigmaG proponujê wartoœæ np. 6 (wedle uznania)
	 * @return przetworzony obrazek.
	 */
	
	public static Mat deblurFilterLR(Mat img, int num_iterations, double sigmaG){		
		
		img.convertTo(img, CvType.CV_64F);
		
		int winSize = (int) (8 * sigmaG + 1) ;
		Mat Y = new Mat();// = img.clone();
		Mat J1 = img.clone();
		Mat J2 = img.clone();
		Mat wI = img.clone(); 
		Mat imR = new Mat(); 
		Mat reBlurred = new Mat();	

		Mat T1, T2, tmpMat1, tmpMat2;
		T1 = new Mat(img.size(), CvType.CV_64F);
		T2 = new Mat(img.size(), CvType.CV_64F);
		tmpMat1=new Mat();
		tmpMat2=new Mat();

		double lambda = 0;
		
		for(int j = 0; j < num_iterations; j++) 
		{		
			if (j>1) {
				// calculation of lambda
				Core.multiply(T1, T2, tmpMat1);
				Core.multiply(T2, T2, tmpMat2);
				lambda=sum(tmpMat1) / (sum(tmpMat2) + EPSILON);
			}
			Core.subtract(J1, J2, Y);
			Core.multiply(Y, new Scalar(lambda), Y);
			Core.add(J1, Y, Y);
			//Y = J1 + lambda * (J1-J2);
			setTho(Y,0);


			// 1)
		    try{
		    	Imgproc.GaussianBlur( Y, reBlurred, new Size(winSize,winSize), sigmaG, sigmaG );//applying Gaussian filter 
		    }
			catch(Exception e)
			{
				System.out.println("Wyj?tek funkcji Imgproc.GaussianBlur:  SlajdBrakRozmycia " + e.toString());
			}
			
			setTho(reBlurred,EPSILON);
			
			// 2)
			
			Core.divide(wI, reBlurred, imR);
			Core.add(imR, new Scalar(EPSILON,EPSILON,EPSILON), imR);

			// 3)
			Imgproc.GaussianBlur( imR, imR, new Size(winSize,winSize), sigmaG, sigmaG );//applying Gaussian filter 

			// 4)
			J2.release();
			J2 = J1.clone();
			J1.release();
			Core.multiply(Y, imR, J1);
			
			T2.release();
			T2 = T1.clone();
			T1.release();
			Core.subtract(J1, Y, T1);
			//T1 = J1 - Y;
			
			tmpMat1.release();
			tmpMat2.release();
			Y.release();
			imR.release();
			reBlurred.release();
			System.gc();
		}
		//Y.release();
		//J1.release();
		J2.release();
		wI.release();
		//imR.release();
		//reBlurred.release();	

		T1.release(); 
		T2.release(); 
		//tmpMat1.release(); 
		//tmpMat2.release();
		
		System.gc();
		// output
		return J1;
	}
	
	/**
	 * Metoda zastêpuje wszystkie elemsy mniejsze od eps wartoœci¹ eps
	 * @param img przetwarzany obrazek
	 * @param eps eps
	 */
	private static void setTho(Mat img, double eps){
		
		int rows = img.rows(); //Calculates number of rows
		int cols = img.cols(); //Calculates number of columns
		int ch = img.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)

		for (int i=0; i<rows; i++)
		{
		    for (int j=0; j<cols; j++)
		    {
		        double[] data = img.get(i, j); //Stores element in an array
		        for (int k = 0; k < ch; k++) //Runs for the available number of channels
		        {
		            if(data[k]<=eps)data[k]=eps;
		        	//data[k] = data[k] * 2; //Pixel modification done here
		        }
		        img.put(i, j, data); //Puts element back into matrix
		    }
		}
	}
	
	/**
	 * Metoda sumuj¹ca piksele
	 * @param img przetwarzany obrazek
	 * @return suma pikseli
	 */
	private static double sum(Mat img){
		
		int rows = img.rows(); //Calculates number of rows
		int cols = img.cols(); //Calculates number of columns
		int ch = img.channels(); //Calculates number of channels (Grayscale: 1, RGB: 3, etc.)
		double s=0;
		for (int i=0; i<rows; i++)
		{
		    for (int j=0; j<cols; j++)
		    {
		        double[] data = img.get(i, j); //Stores element in an array
		        for (int k = 0; k < ch; k++) //Runs for the available number of channels
		        {
		            s= s + data[k];
		        	//data[k] = data[k] * 2; //Pixel modification done here
		        }
		    }
		}
		return s;
	}
	
}
	

