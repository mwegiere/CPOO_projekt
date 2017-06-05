package pl.cpoo;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;

/**
 * Klasa gwarantuje funkcjonalnoœæ:
 * - Filtr górnoprzepustowy o odpowiedzi impulsowej {-1, -1, -1, -1, 9, -1, -1, -1, -1};
 * - Sprawdzenie poziomu ostroœci metod¹ Laplasjanu
 * - Obliczanie FFT (dzia³a tylko dla zerowego kana³u obrazków kolorowych)
 * - Poprawa ostroœci algorytmem LR 
 * @author Kot
 *
 */

public class SlajdBrakRozmycia {
	
	
	
	/**
	 * Stala zawierajaca odpowied impulsow¹ filtru High Pass 
	 */
	private static final float[] impulseResponseHP = {-1, -1, -1, -1, 9, -1, -1, -1, -1};
	/**
	 * Stala zawierajaca odpowiedz impulsow¹ filtru w LR
	 */
	private static final float[] impulseResponseLR = {0,40,0,0,40,0,0,40,0};
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
	 * Tak na prawdê to wszystkie wrzucone zdjêcia slajdów s¹ nieostre. Jedne mniej inne bardziej ale s¹ to ró¿nice na
	 * granicy b³êdu wiêc... no có¿, nie zawsze dzia³a to jak trzeba
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
			System.out.println("Wyj¹tek funkcji Laplacian:  SlajdBrakRozmycia " + e.toString());
		}
	    
	    
	   
	    MatOfDouble mu = new MatOfDouble();
	    MatOfDouble sigma = new MatOfDouble();
	    Core.meanStdDev(out, mu, sigma);
	    double maxLap = sigma.get(0,0)[0]*sigma.get(0,0)[0];
	    
		in.release();
		out.release();
	    return maxLap;
	}
	/**
	 * Metoda poprawiaj¹ca ostroœæ algorytmem LR. Jest on bardzo skuteczny ale dzia³a raczej powoli (Przynajmniej w Javie).
	 * Nie wiem kto robi³ openCV pod Jave... Ogromne wycieki pamiêci, której praktycznie nie idzie zwolniæ. Przetwarzanie 
	 * obrazka o wilkoœci kilkuset KB wymaga ponad 1GB pamiêci?! Lepiej jej nie u¿ywaæ je¿eli to nie jest konieczne.
	 * Lepiej najpierw zbadaæ ostroœæ metod¹ GetSharpness() i sprawdziæ czy nie wystarczy np. u¿yæ filtru górnoprzepustowego.
	 * 
	 * @param img przetwarzany obrazek wejœcie
	 * @param num_iterations iloœæ iteracji (im mniej tym szybciej, pod Cpp mo¿na waln¹æ nawet 100. Tutaj nawet 10 to du¿o)
	 * @param sigmaG proponujê wartoœæ np. 6 (wedle uznania)
	 * @return przetworzony obrazek.
	 */
	
	public static Mat deblurFilterLR(Mat img, int num_iterations, double sigmaG){		
		img.convertTo(img, CvType.CV_64F);
		
		int winSize = (int) (8 * sigmaG + 1) ;
		Mat Y = img.clone();
		Mat J1 = img.clone();
		Mat J2 = img.clone();
		Mat wI = img.clone(); 
		Mat imR = img.clone();  
		Mat reBlurred = img.clone();	

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
				tmpMat1.release();
				tmpMat2.release();
				Core.multiply(T1, T2, tmpMat1);
				Core.multiply(T2, T2, tmpMat2);
				lambda=sum(tmpMat1) / (sum(tmpMat2) + EPSILON);
				//System.out.println(lambda);
				// calculation of lambda
				//return tmpMat1;
			}
			Y.release();
			Core.subtract(J1, J2, Y);
			Core.multiply(Y, new Scalar(lambda), Y);
			Core.add(J1, Y, Y);
			//Y = J1 + lambda * (J1-J2);
			setTho(Y,0);


			// 1)
			//Imgproc.GaussianBlur(src, dst, ksize, sigmaX);
			reBlurred.release();
		    try{
		    	Imgproc.GaussianBlur( Y, reBlurred, new Size(winSize,winSize), sigmaG, sigmaG );//applying Gaussian filter 
		    }
			catch(Exception e)
			{
				System.out.println("Wyj¹tek funkcji Imgproc.GaussianBlur:  SlajdBrakRozmycia " + e.toString());
			}
			
			//reBlurred.setTo(EPSILON , reBlurred <= 0); 
			setTho(reBlurred,EPSILON);
			
			// 2)
			imR.release();
			Core.divide(wI, reBlurred, imR);
			Core.add(imR, new Scalar(EPSILON,EPSILON,EPSILON), imR);
			//imR = imR + EPSILON;

			// 3)
			Imgproc.GaussianBlur( imR, imR, new Size(winSize,winSize), sigmaG, sigmaG );//applying Gaussian filter 

			// 4)
			J2.release();
			J2 = J1.clone();
			Core.multiply(Y, imR, J1);
			
			T2.release();
			T2 = T1.clone();
			Core.subtract(J1, Y, T1);
			//T1 = J1 - Y;
			
		}
		Y.release();
		//Mat J1.release();
		J2.release();
		wI.release();
		imR.release();
		reBlurred.release();	

		T1.release(); 
		T2.release(); 
		tmpMat1.release(); 
		tmpMat2.release();
		
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
	
	
	/**
	 * liczy FFT po kanale zerowym
	 * @param img wejœcie obrazka
	 * @return wyjœciowe FFT obrazka
	 */
	public static Mat fftizer(Mat img) {
		System.out.println(1); 
		ArrayList<Mat> planes=new ArrayList<Mat>();
		ArrayList<Mat> rgb=new ArrayList<Mat>();
		Mat padded = optimizeImageDim(img);
		Core.split(padded, rgb);
		
		rgb.get(1).convertTo(rgb.get(1), CvType.CV_32F);
        planes.add(rgb.get(1));
        planes.add(Mat.zeros(rgb.get(0).size(), CvType.CV_32F));
        Mat complexImage = new Mat();
        // prepare the image planes to obtain the complex image
        // prepare a complex image for performing the dft
        
        Core.merge(planes, complexImage);
        //img.convertTo(img, CvType.CV_32FC1);
        //System.out.println(CvType.CV_32FC1);
        System.out.println(complexImage.type());
        System.out.println(complexImage.channels());
        


        // dft
        Core.dft(complexImage, complexImage);
        Mat magnitude = createOptimizedMagnitude(complexImage);
        return magnitude;
	    
	}
	
	
    private static Mat optimizeImageDim(Mat image)
    {
            // init
            Mat padded = new Mat();
            // get the optimal rows size for dft
            int addPixelRows = Core.getOptimalDFTSize(image.rows());
            // get the optimal cols size for dft
            int addPixelCols = Core.getOptimalDFTSize(image.cols());
            // apply the optimal cols and rows size to the image
            Core.copyMakeBorder(image, padded, 0, addPixelRows - image.rows(), 0, addPixelCols - image.cols(), Core.BORDER_CONSTANT, Scalar.all(0));

            return padded;
    }
    
    /**
     * Optimize the magnitude of the complex image obtained from the DFT, to
     * improve its visualization
     *
     * @param complexImage
     *            the complex image obtained from the DFT
     * @return the optimized image
     */
    private static Mat createOptimizedMagnitude(Mat complexImage)
    {
            // init
            ArrayList<Mat> newPlanes = new ArrayList<Mat>();
            Mat mag = new Mat();
            // split the comples image in two planes
            Core.split(complexImage, newPlanes);
            // compute the magnitude
            Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);

            // move to a logarithmic scale
            Core.add(mag, Scalar.all(1), mag);
            Core.log(mag, mag);
            // optionally reorder the 4 quadrants of the magnitude image
            shiftDFT(mag);
            // normalize the magnitude image for the visualization since both JavaFX
            // and OpenCV need images with value between 0 and 255
            Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX);

            // you can also write on disk the resulting image...
            // Highgui.imwrite("../magnitude.png", mag);

            return mag;
    }
    
    /**
     * Reorder the 4 quadrants of the image representing the magnitude, after
     * the DFT
     *
     * @param image
     *            the {@link Mat} object whose quadrants are to reorder
     */
    private static void shiftDFT(Mat image)
    {
            image = image.submat(new Rect(0, 0, image.cols() & -2, image.rows() & -2));
            int cx = image.cols() / 2;
            int cy = image.rows() / 2;

            Mat q0 = new Mat(image, new Rect(0, 0, cx, cy));
            Mat q1 = new Mat(image, new Rect(cx, 0, cx, cy));
            Mat q2 = new Mat(image, new Rect(0, cy, cx, cy));
            Mat q3 = new Mat(image, new Rect(cx, cy, cx, cy));

            Mat tmp = new Mat();
            q0.copyTo(tmp);
            q3.copyTo(q0);
            tmp.copyTo(q3);

            q1.copyTo(tmp);
            q2.copyTo(q1);
            tmp.copyTo(q2);
    }
	

}
