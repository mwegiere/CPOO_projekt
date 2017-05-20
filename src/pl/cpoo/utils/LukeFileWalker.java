package pl.cpoo.utils;
import java.io.File;
import java.util.Vector;

/**
 * Obiekt tej klasy przechowuje wektor nazw plików z rozszerzeniami okreœlonymi sta³¹ znajduj¹ce siê w katalogu programu
 * 
 * @author Kot
 *
 */

public class LukeFileWalker{
	/**
	 * Sta³a trzymaj¹ca obs³ugiwane typy plików 
	 */
	protected static  final String[] EXTENSIONS = new String[]{
	        ".gif", ".png", ".bmp", ".jpg", ".JPG" // and other formats you need
	    };
	/**
	 * Wektor odczytanych nazw plików
	 */
	protected Vector <String> listOfImages;
	
	/**
	 * konstruktor wyszukuje pliki o zgodnym rozszerzeniu
	 */
	public LukeFileWalker (String SourcesFolderName)
	{
		
		listOfImages = new Vector<String>();
		try{
			File sourceFolder = new File(System.getProperty("user.dir") + File.separator + SourcesFolderName);
			File[] listOfFiles = sourceFolder.listFiles();
			
			    for (int i = 0; i < listOfFiles.length; i++) 
			    {
			    	if (listOfFiles[i].isFile()) 
			    	{
			    		for (final String ext : EXTENSIONS) 
			    			if (listOfFiles[i].getName().endsWith(ext))
			    			{ 
			    				//System.out.println("File " + listOfFiles[i].getName());  
			    				listOfImages.addElement(listOfFiles[i].getName());
			    			}
			    	}
			    }
			}
			catch(Exception e)
			{
				System.out.println("Wyj¹tek podczas inicjalizacji:  LukeFileWraper " + e.toString());
			}
	}
	
	/**
	 * 
	 * @return Wektor nazw znalezionych plików.
	 */
	public Vector<String> get() {return listOfImages;}
	
}