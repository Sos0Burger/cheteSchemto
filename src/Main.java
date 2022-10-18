import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;


public class Main{
    public final static String input = "input.txt";
    public static String musicSite;
    public static String picSite;
    public static String picOutput;
    public static String musicOutput;

    public static void main(String[] args) {
        //разделение текста из файла
        try(BufferedReader br = new BufferedReader(new FileReader(input))){
            String line = br.readLine();
            musicSite = line.split(" ")[0];
            musicOutput = line.split(" ")[1];
            line = br.readLine();
            picSite = line.split(" ")[0];
            picOutput = line.split(" ")[1];
        }
        catch (IOException ex){
            System.out.println(ex.getMessage());
        }
        MusicDownload musicDownload = new MusicDownload(musicSite, musicOutput);
        PicDownload picDownload =new PicDownload(picSite, picOutput);

        musicDownload.start();
        picDownload.start();

    }
}

class DownloadThread extends Thread{
    String link;
    String file;

    public void run() {
        try{
            URL url = new URL(link);

            URLConnection uc = url.openConnection();
            uc.connect();
            uc = url.openConnection();
            uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

            ReadableByteChannel byteChannel = Channels.newChannel(uc.getInputStream());
            FileOutputStream stream = new FileOutputStream(file);
            stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
            stream.close();
            byteChannel.close();
            System.out.println("Файл "+ file+" скачан");
            if(file.equals("Music/0-.mp3")){
                try (FileInputStream inputStream = new FileInputStream("Music/0.mp3")) {
                    try {
                        Player player = new Player(inputStream);
                        player.play();
                    } catch (JavaLayerException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }
    }

    DownloadThread(String link, String file){
        this.link=link;
        this.file=file;
    }
}
class MusicDownload extends Thread{
    public ArrayList<DownloadThread> threadArrayList = new ArrayList<>();
    String site;
    String output;
    @Override
    public void run() {

        try(FileWriter fw = new FileWriter("musicOutputUrls.txt")){

            StringBuilder siteHTML = new StringBuilder();

            URL url = new URL(site);
            URLConnection uc = url.openConnection();
            uc.connect();
            uc = url.openConnection();
            uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

            try(BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()))){
                int i;
                while((i = br.read())!=-1 ){
                    siteHTML.append((char)i);
                }
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
            //руглярка для ссылок на страницу со скачиванием
            Pattern pattern = Pattern.compile("data-url=\"/get-music/(([^\"])*)\"");
            Matcher matcher = pattern.matcher(siteHTML);
            //регулярка для ссылок на скачивание
            Pattern patternMusic = Pattern.compile("\"/download\\.php\\?file=(([^\"])*)\"");
            //ссылки на страницу со скачиванием
            while(matcher.find()){
                URL downloadUrl = new URL(site+matcher.group().replaceAll("\"|\\\\", "").replace("data-url=",""));
                URLConnection ucDownload = downloadUrl.openConnection();
                ucDownload.connect();
                ucDownload = downloadUrl.openConnection();
                ucDownload.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
                try(BufferedReader br = new BufferedReader(new InputStreamReader(ucDownload.getInputStream()))){
                    StringBuilder siteHTMLDownload = new StringBuilder();
                    int i;
                    while((i = br.read())!=-1 ){
                        siteHTMLDownload.append((char)i);
                    }

                    Matcher matcherMusic = patternMusic.matcher(siteHTMLDownload);
                    if(matcherMusic.find()) {
                        fw.write(matcherMusic.group().replaceAll("\"|\\\\", "") + "\n");
                        fw.flush();
                    }
                }

            }

        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        //скачка файлов по ссылкам
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("musicOutputUrls.txt"))){
            String link;
            int counter = 0;
            while((link = bufferedReader.readLine())!=null){
                threadArrayList.add(new DownloadThread(site+link,output + counter+".mp3"));
                threadArrayList.get(counter).start();
                counter++;
            }
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }
    }
    MusicDownload(String site, String output){
        this.output = output;
        this.site = site;
    }
}
class PicDownload extends Thread{
    public ArrayList<DownloadThread> threadArrayList = new ArrayList<>();
    String site;
    String picOutput;
    @Override
    public void run() {

        try(FileWriter fw = new FileWriter("picOutputUrls.txt")){
            URL url = new URL(site);

            StringBuilder siteHTML = new StringBuilder();
            URLConnection uc = url.openConnection();
            uc.connect();
            uc = url.openConnection();
            uc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
            try(BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()))){
                int i;
                while((i = br.read())!=-1 ){
                    siteHTML.append((char)i);
                }
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
            Pattern pattern = Pattern.compile("src=\"https://img.freepik.com/([A-z1-9/\\-=\\s\\.]*)\\.jpg");
            Matcher matcher = pattern.matcher(siteHTML);

            while (matcher.find()){
                fw.write(matcher.group().replaceAll("\"|\\?|src=", "")+"\n");
                fw.flush();
            }
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        try(BufferedReader bufferedReader = new BufferedReader(new FileReader("picOutputUrls.txt"))){
            String link;
            int counter = 0;
            while((link = bufferedReader.readLine())!=null){
                threadArrayList.add(new DownloadThread(link,picOutput + counter+".jpg"));
                threadArrayList.get(counter).start();
                counter++;
            }
        }
        catch (Exception ex){
            System.out.println(ex.getMessage());
        }
    }
    PicDownload(String site, String picOutput){
        this.picOutput = picOutput;
        this.site = site;
    }
}