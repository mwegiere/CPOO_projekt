package pl.cpoo;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SlajdBezSzumu {

    private Mat InImg;
    private static int progress = 0;

    // tablica LUT dla funkcji exp(-x)
    private static double[] expLUT;
    private static double LUTprecision = 1000.0;
    private static double LUTmax = 30.0;

    // pr�g pomijalnie ma�ej liczby double
    private static double TINY_DOUBLE = 0.00000001;
    //private static double LARGE_DOUBLE = 100000000.0;

    // parametry algorytmu non local means
    public static int halfPatchSize = 1;
    public static int halfWindowSize = 4;
    public static double sigma = 40;
    public static double filterParam = 0.4;

    /*
     * konstruktor pobieraj�cy obraz
     */
    public SlajdBezSzumu(Mat InImg_) {
        InImg = InImg_;
    }

    /**
     * Inicjuje tablic� LUT dla exp(-x)
     */
    private void fillexpLUT() {
        int size = (int) Math.round(LUTmax * LUTprecision);
        expLUT = new double[size];
        for (int i = 0; i < size; i++)
            expLUT[i] = Math.exp(-(double) i / LUTprecision);
    }

    /**
     * Zwraca warto�� funkcji exp(-x) dla podanego argumentu na podstawie tabeli LUT
     * @param x argument
     * @return wartość
     */
    private double get_expLUT(double x) {
        if (x >= LUTmax - 1.0) return 0.0;

        int index = (int) Math.ceil(x * LUTprecision);
        double y1 = expLUT[index];
        double y2 = expLUT[index + 1];
        return y1 + (y2 - y1) * (x * LUTprecision - (double) index);
    }

    /**
     * Wype�nia 3-wymiarow� tablic� warto�ci double warto�ciami 0.0
     * @param xsize kolumny
     * @param ysize wiersze
     * @param channels kana�y
     * @param array tablica
     */
    private void clear3DoubleArray(int xsize, int ysize, int channels, double[][][] array) {
        for (int x = 0; x < xsize; x++) {
            for (int y = 0; y < ysize; y++) {
                for (int ch = 0; ch < channels; ch++) {
                    array[x][y][ch] = 0.0;
                }
            }
        }
    }

    /**
     * Wype�nia 2-wymiarow� tablic� warto�ci double warto�ciami 0.0
     * @param xsize kolumny
     * @param ysize wiersze
     * @param array tablica
     */
    private void clear2DoubleArray(int xsize, int ysize, double[][] array) {
        for (int x = 0; x < xsize; x++) {
            for (int y = 0; y < ysize; y++) {
                array[x][y] = 0.0;
            }
        }
    }

    /**
     * Inicjalizacja tablicy double warto�ciami z macierzy
     * @param array tablica
     * @param mat macierz warto�ci
     */
    private void initDoubleArray(double[][][] array, Mat mat) {
        for (int x = 0; x < mat.width(); x++) {
            for (int y = 0; y < mat.height(); y++) {
                double[] data = mat.get(y, x);
                for (int ch = 0; ch < mat.channels(); ch++) {
                    array[x][y][ch] += data[ch];
                }

            }
        }
    }

    /**
     * Konwertuje 3-wymiarowa tablice double na macierz
     * @param rows
     * @param cols
     * @param channels
     * @param array tablica
     * @return now� macierz
     */
    private Mat doubleArrayToMat(int cols, int rows, int channels, int type, double[][][] array) {
        Mat newMat = Mat.zeros(rows, cols, CvType.CV_64FC(channels));
        double[] data = newMat.get(0, 0);
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                for (int ch = 0; ch < channels; ch++) {
                    data[ch] = array[x][y][ch];
                }
                newMat.put(y, x, data);
            }
        }
        Mat converted = new Mat();
        newMat.convertTo(converted, type);
        return converted;
    }

    /**
     * Inicjalizuje obraz ca�kowany (zmianna pomocnicza do akceleracji algorytmu),
     * ka�dy piksel w tym obraazie jest r�wny
     * sumie kwadratow wszystkich pikseli z gornego lewego fragmentu obrazu (po wszystkich kana�ach),
     * kt�ry ogranicza ten piksel
     * @param cols szerokosc obrazu oryginalnego
     * @param rows wysokosc obrazu oryginalnego
     * @param channels ilosc kanalow obrazu oryginalnego
     * @param inImg obraz oryginalny
     * @param integralImg obraz sca�kowany
     */
    private void prepareSSI2Image(int cols, int rows,
                                  int channels,
                                  double[][][] inImg,
                                  double[][] integralImg) {
        if (cols <= 0 || rows <= 0) return;

        integralImg[0][0] = 0.0;
        for (int ch = 0; ch < channels; ch++) {
            integralImg[0][0] += inImg[0][0][ch] * inImg[0][0][ch];
        }

        for (int x = 1; x < cols; x++) {
            integralImg[x][0] = integralImg[x - 1][0];
            for (int ch = 0; ch < channels; ch++) {
                integralImg[x][0] += inImg[x][0][ch] * inImg[x][0][ch];
            }
        }

        for (int y = 1; y < rows; y++) {
            integralImg[0][y] = integralImg[0][y - 1];
            for (int ch = 0; ch < channels; ch++) {
                integralImg[0][y] += inImg[0][y][ch] * inImg[0][y][ch];
            }
        }

        for (int x = 1; x < cols; x++) {
            for (int y = 1; y < rows; y++) {
                integralImg[x][y] = integralImg[x - 1][y] + integralImg[x][y - 1] - integralImg[x - 1][y - 1];
                for (int ch = 0; ch < channels; ch++) {
                    integralImg[x][y] += inImg[x][y][ch] * inImg[x][y][ch];
                }
            }
        }
    }

    /**
     * Oblicza sum� kwadrat�w r�nic warto�ci pikseli w kawa�ku
     * obrazu po wszystkich kana�ach obrazu
     * @param x wsp�rz�dna x �rodka kawa�ka 1
     * @param y wsp�rz�dna y �rodka kawa�ka 1
     * @param i wsp�rz�dna x �rodka kawa�ka 2
     * @param j wsp�rz�dna y �rodka kawa�ka 2
     * @param halfPatchSize po�owa boku kawa�ka
     * @param channels ilosc kana��w
     * @param inImgArr obraz wejsciowy
     * @return suma kwadrat�w r�nic
     */
    private double patchDiff(int x, int y,
                             int i, int j,
                             int halfPatchSize,
                             int channels,
                             double[][][] inImgArr) {
        double diff = 0.0;

        for (int xx = -halfPatchSize; xx <= halfPatchSize ; xx++) {
            for (int yy = -halfPatchSize; yy <= halfPatchSize; yy++) {

                for (int ch = 0; ch < channels; ch++) {
                    double pix1 = inImgArr[xx + x][yy + y][ch];
                    double pix2 = inImgArr[xx + i][yy + j][ch];
                    double pixdiff = pix1 - pix2;
                    diff += pixdiff*pixdiff;
                }
            }
        }
        return diff;
    }

    /**
     * Dodaje warto�� z kawa�ka o pozycji x, y macierzy wej�ciowej do warto�ci
     * kawa�ka o pozycji x, y macierzy wyj�ciowej dla wszystkich kana��w
     * @param halfPatchSize po�owa rozmiaru kawa�ka
     * @param channels ilo�� kana��w
     * @param weight waga warto�ci
     * @param inImgArr macierz wej�ciowa
     * @param patchArr macierz kawa�ka
     */
    private void addToPatch(int x, int y,
                            int halfPatchSize,
                            int channels,
                            double weight,
                            double[][][] inImgArr,
                            double[][][] patchArr) {
        for (int xx = -halfPatchSize; xx <= halfPatchSize; xx++) {
            for (int yy = -halfPatchSize; yy <= halfPatchSize ; yy++) {
                for (int ch = 0; ch < channels; ch++) {
                    patchArr[xx + halfPatchSize][yy + halfPatchSize][ch] +=
                            weight * inImgArr[xx + x][yy + y][ch];
                }
            }
        }
    }

    /**
     * Algorytm odszumiania non-local means denoising
     *
    * @param halfPatchSize po�owa rozmiaru fragmentu szukanego
     * @param halfWindowSize po�owwa rozmiaru okna przeszukiwania
     * @param sigma parametr szumu
     * @param filterParam parametr filtrowania
     * @param inImg obraz wej�ciowy
     * @return obraz odszumiony
     */
    private Mat NonLocalMeansDenoising(int halfPatchSize,
                              int halfWindowSize,
                              double sigma,
                              double filterParam,
                              Mat inImg) {
        // pixels nr
        int nr_pixels = inImg.width() * inImg.height();
        //Random rand = new Random();


        // inicjalizacja parametr�w algorytmu
        int channels = inImg.channels();
        double sigma2 = sigma * sigma;
        double H = filterParam * sigma;
        double H2 = H * H;
        int imgWidth = inImg.width();
        int imgHeight = inImg.height();
        int nr_patchPixels_allc = channels * (2 * halfPatchSize + 1) * (2 * halfPatchSize + 1);
        int patchSideLen = 2 * halfPatchSize + 1;

        // inicjalizacja tablicy LUT
        fillexpLUT();

        // alokacja pami�ci na obraz wejsciowy
        double [][][] inImgArray = new double[imgWidth][imgHeight][channels];
        initDoubleArray(inImgArray, inImg);

        // inicjalizacja obrazu sca�kowanego kwadratowo
//        double SSI2_ImgArray[][] = new double[imgWidth][imgHeight];
//        prepareSSI2Image(imgWidth, imgHeight, channels, inImgArray, SSI2_ImgArray);

        // alokacja pami�ci na obraz odszumiony
        double [][][] outImgArray = new double[imgWidth][imgHeight][channels];
        clear3DoubleArray(imgWidth, imgHeight, channels, outImgArray);

        // Utworzenie macierzy pomocniczej zliczaj�cej
        double [][] counterArray = new double[imgWidth][imgHeight];
        clear2DoubleArray(imgWidth, imgHeight, counterArray);

        // G��wna p�tla algorytmu
        IntStream.range(0, imgHeight).parallel().forEach(y -> {
            System.out.print( String.format("denoising progress: %.2f %%...\r", 100 * (float) progress / (float) nr_pixels) );
            // Utworzenie macierzy pomocniczej dla kawa�ka obrazu
            double [][][] patchArray = new double[patchSideLen][patchSideLen][channels];
            clear3DoubleArray(patchSideLen, patchSideLen, channels, patchArray);

            for (int x = 0; x < imgWidth; x++) {
                progress++;
             // Ustalenie rozmiaru kawa�ka
                int halfPathSize0 = Math.min(halfPatchSize,
                                    Math.min(imgWidth - 1 - x,
                                    Math.min(imgHeight - 1 - y,
                                    Math.min(x, y))));

                // Ustalenie rozmiar�w przeszukiwanego okna
                int imin = Math.max(x - halfWindowSize, halfPathSize0);
                int jmin = Math.max(y - halfWindowSize, halfPathSize0);

                int imax = Math.min(x + halfWindowSize, imgWidth - 1 - halfPathSize0);
                int jmax = Math.min(y + halfWindowSize, imgHeight - 1 - halfPathSize0);

                double maxWeight = 0.0;
                double sumWeights = 0.0;

                for (int j = jmin; j <= jmax; j++) {
                    for (int i = imin; i <= imax; i++) {
                        if (i != x || j != y) {

                            double diff = patchDiff(x, y, i, j, halfPathSize0, channels, inImgArray);
                            // diff^2 - 2 * sigma^2 * N
                            diff = Math.max(diff - 2.0f * (double) nr_patchPixels_allc * sigma2, 0.0f);
                            diff = diff / H2;

                            double weight = get_expLUT(diff);

                            if (weight > maxWeight)
                                maxWeight = weight;

                            sumWeights += weight;

                            // Dodaj wa�ona warto�� z obrazka wejsciowego do macierzy kawa�ka
                            addToPatch(i, j, halfPathSize0, channels, weight, inImgArray, patchArray);
                        }
                    }
                } // end of i, j loop

                // Dodaj maksymalnie wa�ona warto�� z obrazka wejsciowego na wyjsciowy
                addToPatch(x, y, halfPathSize0, channels, maxWeight, inImgArray, patchArray);

                sumWeights += maxWeight;

                if (sumWeights > TINY_DOUBLE) {
                    for (int i = -halfPathSize0; i <= halfPathSize0; i++) {
                        for (int j = -halfPathSize0; j <= halfPathSize0; j++) {

                            counterArray[i + x][j + y] += 1.0;

                            for (int ch = 0; ch < channels; ch++) {
                                outImgArray[i+x][j+y][ch] += patchArray[i+halfPathSize0][j+halfPathSize0][ch] / sumWeights;
                            }
                        }
                    }
                } // endif
                clear3DoubleArray(patchSideLen, patchSideLen, channels, patchArray);
            }
        }); // end of parallel scope

        for (int x = 0; x < imgWidth; x++) {
            for (int y = 0; y < imgHeight; y++) {
                double count = counterArray[x][y];
                if (count > 0.0) {
                    for (int ch = 0; ch < channels; ch++) {
                        outImgArray[x][y][ch] /= count;
                    }
                } else {
                    for (int ch = 0; ch < channels; ch++) {
                        outImgArray[x][y][ch] = inImgArray[x][y][ch];
                    }
                }
            }
        }
        return doubleArrayToMat(imgWidth, imgHeight, channels, inImg.type(), outImgArray);
    }
    /*
     * operacja na obrazie
     */
    private Mat SlajdBezSzumuAction() {
        long startTime = System.nanoTime();
        progress = 0;
        Mat out = NonLocalMeansDenoising(halfPatchSize, halfWindowSize,
                sigma, filterParam, InImg);
        long estimatedTime = System.nanoTime() - startTime;
        long millis = estimatedTime / 1000000;
        System.out.println("Denoising time performance: " + String.format(
                "%d min, %d.%d sec",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)),
                millis % 1000
        )
        );
        return out;
    }

    /*
     * getter poprzez kt�ry przekazujemy obraz do kolejnej klasy realizuj�cej
     * nast�pny krok toku przetwarzania
     */
    public Mat getImg() {
        return SlajdBezSzumuAction();
    }

}
