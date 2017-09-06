import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SlajdBezSzumu {

    private Mat InImg;
    private static int progress = 0;

    // tablica LUT dla funkcji exp(-x)
    private static double[] expLUT;
    private static double LUTprecision = 1000.0;
    private static double LUTmax = 30.0;

    // próg pomijalnie małej liczby double
    private static double TINY_DOUBLE = 0.00000001;
    private static double LARGE_DOUBLE = 100000000.0;

    // parametry algorytmu non local means
    public static int halfPatchSize = 1;
    public static int halfWindowSize = 5;
    public static double sigma = 15.0;
    public static double filterParam = 0.4;

    /*
     * konstruktor pobierający obraz
     */
    public SlajdBezSzumu(Mat InImg_) {
        InImg = InImg_;
    }

    /**
     * Inicjuje tablicę LUT dla exp(-x)
     */
    private void fillexpLUT() {
        int size = (int) Math.round(LUTmax * LUTprecision);
        expLUT = new double[size];
        for (int i = 0; i < size; i++)
            expLUT[i] = Math.exp(-(double) i / LUTprecision);
    }

    /**
     * Zwraca wartość funkcji exp(-x) dla podanego argumentu na podstawie tabeli LUT
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
     * Wypełnia 3-wymiarową tablicę wartości double wartościami 0.0
     * @param xsize kolumny
     * @param ysize wiersze
     * @param channels kanały
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
     * Wypełnia 2-wymiarową tablicę wartości double wartościami 0.0
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
     * Inicjalizacja tablicy double wartościami z macierzy
     * @param array tablica
     * @param mat macierz wartości
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
     * @return nową macierz
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
     * Inicjalizuje obraz całkowany (zmianna pomocnicza do akceleracji algorytmu),
     * każdy piksel w tym obraazie jest równy
     * sumie kwadratow wszystkich pikseli z gornego lewego fragmentu obrazu (po wszystkich kanałach),
     * który ogranicza ten piksel
     * @param cols szerokosc obrazu oryginalnego
     * @param rows wysokosc obrazu oryginalnego
     * @param channels ilosc kanalow obrazu oryginalnego
     * @param inImg obraz oryginalny
     * @param integralImg obraz scałkowany
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
     * Oblicza sumę kwadratów różnic wartości pikseli w kawałku
     * obrazu po wszystkich kanałach obrazu
     * @param x współrzędna x środka kawałka 1
     * @param y współrzędna y środka kawałka 1
     * @param i współrzędna x środka kawałka 2
     * @param j współrzędna y środka kawałka 2
     * @param halfPatchSize połowa boku kawałka
     * @param channels ilosc kanałów
     * @param inImgArr obraz wejsciowy
     * @return suma kwadratów różnic
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
     * Dodaje wartość z kawałka o pozycji x, y macierzy wejściowej do wartości
     * kawałka o pozycji x, y macierzy wyjściowej dla wszystkich kanałów
     * @param halfPatchSize połowa rozmiaru kawałka
     * @param channels ilość kanałów
     * @param weight waga wartości
     * @param inImgArr macierz wejściowa
     * @param patchArr macierz kawałka
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
     * @param halfPatchSize połowa rozmiaru fragmentu szukanego
     * @param halfWindowSize połowwa rozmiaru okna przeszukiwania
     * @param sigma parametr szumu
     * @param filterParam parametr filtrowania
     * @param inImg obraz wejściowy
     * @return obraz odszumiony
     */
    private Mat NonLocalMeansDenoising(int halfPatchSize,
                              int halfWindowSize,
                              double sigma,
                              double filterParam,
                              Mat inImg) {
        // pixels nr
        int nr_pixels = inImg.width() * inImg.height();
        Random rand = new Random();

        // inicjalizacja parametrów algorytmu
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

        // alokacja pamięci na obraz wejsciowy
        double [][][] inImgArray = new double[imgWidth][imgHeight][channels];
        initDoubleArray(inImgArray, inImg);

        // inicjalizacja obrazu scałkowanego kwadratowo
//        double SSI2_ImgArray[][] = new double[imgWidth][imgHeight];
//        prepareSSI2Image(imgWidth, imgHeight, channels, inImgArray, SSI2_ImgArray);

        // alokacja pamięci na obraz odszumiony
        double [][][] outImgArray = new double[imgWidth][imgHeight][channels];
        clear3DoubleArray(imgWidth, imgHeight, channels, outImgArray);

        // Utworzenie macierzy pomocniczej zliczającej
        double [][] counterArray = new double[imgWidth][imgHeight];
        clear2DoubleArray(imgWidth, imgHeight, counterArray);

        // Główna pętla algorytmu
        IntStream.range(0, imgHeight).parallel().forEach(y -> {
            System.out.println( String.format("denoising progress: %.2f %%...", 100 * (float) progress / (float) nr_pixels) );
            // Utworzenie macierzy pomocniczej dla kawałka obrazu
            double [][][] patchArray = new double[patchSideLen][patchSideLen][channels];
            clear3DoubleArray(patchSideLen, patchSideLen, channels, patchArray);

            for (int x = 0; x < imgWidth; x++) {
                progress++;
                // Ustalenie rozmiaru kawałka
                int halfPathSize0 = Math.min(halfPatchSize,
                                    Math.min(imgWidth - 1 - x,
                                    Math.min(imgHeight - 1 - y,
                                    Math.min(x, y))));

                // Ustalenie rozmiarów przeszukiwanego okna
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

                            // Dodaj ważona wartość z obrazka wejsciowego do macierzy kawałka
                            addToPatch(i, j, halfPathSize0, channels, weight, inImgArray, patchArray);
                        }
                    }
                } // end of i, j loop

                // Dodaj maksymalnie ważona wartość z obrazka wejsciowego na wyjsciowy
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
     * getter poprzez który przekazujemy obraz do kolejnej klasy realizującej
     * następny krok toku przetwarzania
     */
    public Mat getImg() {
        return SlajdBezSzumuAction();
    }

}
