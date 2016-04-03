package Rss_Information_Retrieval;
import java.io.FileNotFoundException;
import java.net.URL;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import it.sauronsoftware.feed4j.FeedException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
public class RssUtil
{
    public ArrayList<URL> gatherFeeds(String feeds_path) throws FileNotFoundException, IOException
    {
        ArrayList url_list = new ArrayList();
        FileReader fstream = new FileReader(feeds_path);
        BufferedReader in = new BufferedReader(fstream);
        String line;
        int count = 0;
        while ((line = in.readLine()) != null)
        {
            count++;
            try
            {
                URL url = new URL(line.substring(0, line.length()-1));
                url_list.add(url);
            }
            catch (Exception e)
            {
                System.err.println("Error with "+line+" format in line "+count);
                continue;
            }
        }
        return url_list;
    }
    
    public String DownloadRssFeed(URL url) throws MalformedURLException, IOException, IllegalArgumentException, FeedException, com.sun.syndication.io.FeedException
    {
        try
        {
            XmlReader reader = null;
            reader = new XmlReader(url);
            SyndFeed feed = new SyndFeedInput().build(reader);
            reader = new XmlReader(url);
            File file = new File("RssFeeds");
            file.mkdir();
            FileWriter fstream = new FileWriter("RssFeeds/"+feed.getTitle()+".xml");
            BufferedWriter out = new BufferedWriter(fstream);
            int c;
            while((c = reader.read())!= -1)
            {
                out.write(c);
            }
            out.close();
            return feed.getTitle();
        }
        catch (Exception e)
        {
            System.err.println("Didn't add "+url.toString());
            return null;
        }
    }
}
