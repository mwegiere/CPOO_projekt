package pl.cpoo;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Size;
import java.lang.Math;
import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;


public class SlajdRownomierneOswietlenie {

	private Mat InImg;
	private Mat InMask;
	private Mat OutImgYcbCr;
	private Mat OutImgRGB;
	Mat InImgYcbCr;
	Mat MaskYcbCr;

	/*
	 * konstruktor pobieraj¹cy obraz
	 */
	public SlajdRownomierneOswietlenie(Mat InImg_, Mat InMask_) {
		InImg = InImg_.clone();
		InMask = InMask_.clone();
		OutImgYcbCr = InImg_.clone();
		InImgYcbCr = InImg_.clone();
		MaskYcbCr = InMask_.clone();
		OutImgRGB = InMask_.clone();
	}

	public BufferedImage Mat2BufferedImage(Mat m) {

		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;
	}

	public void displayImage(Image img2) {
		ImageIcon icon = new ImageIcon(img2);
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.setSize(img2.getWidth(null) + 50, img2.getHeight(null) + 50);
		JLabel lbl = new JLabel();
		lbl.setIcon(icon);
		frame.add(lbl);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	public Mat histeq_lum(Mat img) {
		double max, min;
		max = img.get(0, 0)[0];
		min = img.get(0, 0)[0];
		for (int i = 0; (int) i < img.height(); ++i) {
			for (int j = 0; (int) j < img.width(); ++j) {
				if (img.get(i, j)[0] < min)
					min = img.get(i, j)[0];
				if (img.get(i, j)[0] > max)
					max = img.get(i, j)[0];
			}
		}
		double[] tmp;
		for (int i = 0; (int) i < img.height(); ++i) {
			for (int j = 0; (int) j < img.width(); ++j) {
				tmp = img.get(i, j);
				tmp[0] = img.get(i, j)[0]/max * (max - min) + min;
				img.put(i, j, tmp);
			}
		}
		return img;
	}

	public Mat histeq(Mat img) {
		double maxR, minR, maxG, minG, maxB, minB;
		maxR = img.get(0, 0)[0];
		minR = img.get(0, 0)[0];
		maxG = img.get(0, 0)[1];
		minG = img.get(0, 0)[1];
		maxB = img.get(0, 0)[2];
		minB = img.get(0, 0)[2];
		for (int i = 0; (int) i < img.height(); ++i) {
			for (int j = 0; (int) j < img.width(); ++j) {
				if (img.get(i, j)[0] < minR)
					minR = img.get(i, j)[0];
				if (img.get(i, j)[0] > maxR)
					maxR = img.get(i, j)[0];
				if (img.get(i, j)[1] < minG)
					minG = img.get(i, j)[1];
				if (img.get(i, j)[1] > maxG)
					maxG = img.get(i, j)[1];
				if (img.get(i, j)[2] < minB)
					minB = img.get(i, j)[2];
				if (img.get(i, j)[2] > maxB)
					maxB = img.get(i, j)[2];
			}
		}
		double[] tmp;
		for (int i = 0; (int) i < img.height(); ++i) {
			for (int j = 0; (int) j < img.width(); ++j) {
				tmp = img.get(i, j);
				tmp[0] = tmp[0]/maxR * (maxR - minR) + minR;
				tmp[1] = tmp[1]/maxG * (maxG - minG) + minG;
				tmp[2] = tmp[2]/maxB * (maxB - minB) + minB;
				img.put(i, j, tmp);
			}
		}
		return img;
	}

	private Mat SlajdRownomierneOswietlenieAction() {
		
		Size SizeInImg = InImg.size();
		Size SizeMask = InMask.size();

		System.out.println(SizeMask.height);
		System.out.println(SizeMask.width);

		Imgproc.cvtColor(InImg, InImgYcbCr, Imgproc.COLOR_RGB2YCrCb);
		Imgproc.cvtColor(InMask, MaskYcbCr, Imgproc.COLOR_RGB2YCrCb);
		
		InImgYcbCr = histeq_lum(InImgYcbCr.clone());
		MaskYcbCr = histeq_lum(MaskYcbCr.clone());

		int height = Math.min((int) SizeInImg.height, (int) SizeMask.height);
		int width = Math.min((int) SizeInImg.width, (int) SizeMask.width);

		int licznik = 0;
		double suma = 0;
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++) {
				double[] DataMaskYcbCr = MaskYcbCr.get(i, j);
				suma = suma + DataMaskYcbCr[0];
				licznik = licznik + 1;
			}
		double srednia = suma/licznik;
		suma = 0;
		licznik = 0;
		
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++) {
				double[] DataMaskYcbCr = MaskYcbCr.get(i, j);
				if (DataMaskYcbCr[0]<srednia){
					suma = suma + DataMaskYcbCr[0];
					licznik = licznik + 1;
				}
			}		
		double srednia_ciemnego = suma/licznik;
		
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++) {
				double[] DataOriginalYcbCr = InImgYcbCr.get(i, j);
				double[] DataMaskYcbCr = MaskYcbCr.get(i, j);

				double MaskaY = DataMaskYcbCr[0];
				//double InImgY = DataOriginalYcbCr[0];
				double roznica = MaskaY - srednia_ciemnego;
				
				DataOriginalYcbCr[0] = DataOriginalYcbCr[0] - roznica;

				OutImgYcbCr.put(i, j, DataOriginalYcbCr);
			}
		
		Imgproc.cvtColor(OutImgYcbCr, OutImgRGB, Imgproc.COLOR_YCrCb2RGB);
		
		InImg.release();
		InMask.release();
		OutImgYcbCr.release();
		InImgYcbCr.release();
		MaskYcbCr.release();
		
		return OutImgRGB;

	}

	/*
	 * getter poprzez który przekazujemy obraz do kolejnej klasy realizuj¹cej
	 * nastêpny krok toku przetwarzania
	 */
	public Mat getImg() {
		return SlajdRownomierneOswietlenieAction();
	}

}