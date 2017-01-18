package uccm;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GetUccm {

    public static void main(String[] argv) {
        try {

            URL uccm100Url = new URL("https://github.com/alexeysudachen/uccm/archive/uccm100.zip");

            System.out.println("it uses source "+uccm100Url.toString());

            File uccmRepo;

	    if ( System.getenv("UCCM100REPO") != null ) {
                uccmRepo = new File(System.getenv("UCCM100REPO"));
	    } else {
                File appData = new File(System.getenv("LOCALAPPDATA"));

                if ( !appData.exists() ) {
                    System.err.println("could not find local AppData");
                    System.exit(1);
                }

                uccmRepo = new File(appData,"uCcm100Repo");
            }

            if ( !uccmRepo.exists() )
                uccmRepo.mkdir();

            System.out.println("it uses UCCM repo "+uccmRepo.getAbsolutePath());

            File uccm100Zip = File.createTempFile("uccm100", ".zip");  
            uccm100Zip.deleteOnExit();
            OutputStream os = new FileOutputStream(uccm100Zip);
            byte[] buffer = new byte[102400];
            int totalBytesRead = 0;
            int bytesRead = 0;

            System.out.println("connecting...");
            URLConnection connection = uccm100Url.openConnection();
            InputStream is = connection.getInputStream();

            System.out.println("getting ZIP-archive ...");

            while ((bytesRead = is.read(buffer)) > 0) {
                os.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            os.close();
            is.close();

            System.out.println("unpacking ...");

            ZipFile zf = new ZipFile(uccm100Zip);
            for (Enumeration zfe = zf.entries(); zfe.hasMoreElements(); ) {
                ZipEntry r = (ZipEntry)zfe.nextElement();
                File f = new File(uccmRepo,r.getName());
                if ( r.isDirectory() ) {
                    if (!f.exists()) f.mkdir();
                } else {
                    os = new FileOutputStream(f);
                    is = zf.getInputStream(r);
                    int len = is.read(buffer);
                    while (len >= 0) {
                        os.write(buffer, 0, len);
                        len = is.read(buffer);
                    }
                    is.close();
                    os.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
