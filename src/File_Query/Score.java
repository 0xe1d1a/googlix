package File_Query;

import File_Information_Retrieval.gui;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class Score 
{
    private HashMap<String, VocabularyInfo> map = new HashMap<String, VocabularyInfo>();  
    private HashMap<Integer, HashMap<String, Integer>> query_map;
    private HashMap<String, Integer> qtf_map;
    private HashMap<Integer,DocumentInfo> file_dftf;
    private HashMap<String, String> snippet_map;
    private HashMap<String, SyndEntry> item_map;
    private ArrayList<ScoreSet> file_scores;
    
    private HashMap<Integer,String> files;
    private double qtf;
    private double maxfreq;
    public int mode = 1;
    
    public void ScanVocabulary(String voc_path) throws IOException
    {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(voc_path));
        String line = null;
        String[] parts;
        while ((line = reader.readLine()) != null)
        {
            parts = line.split(" ");
            map.put(parts[0], new VocabularyInfo(Double.parseDouble(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        }
        System.out.println("Ready:");
    }
    private void CalculateQnorm()
    {
        Iterator<String> i = qtf_map.keySet().iterator();
        while (i.hasNext()) 
        {
            String str = i.next();
            double idf = map.get(str).idf;
            qtf+= Math.pow((qtf_map.get(str)/maxfreq) * idf,2);
        }
        qtf = Math.sqrt(qtf);
    }
    
    private void CalculateScore()
    {
        Iterator<Integer> it;
        it = query_map.keySet().iterator();
        while (it.hasNext()) //for each file
        {
            Integer integer = it.next();
            Iterator<String> it3 = query_map.get(integer).keySet().iterator();
            double sum = 0.0;
            while (it3.hasNext()) //for each term
            {
                String string = it3.next();
                double d =  (query_map.get(integer).get(string)/(double)file_dftf.get(integer).maxtf)  * (map.get(string).idf) ;
                double q =  ((qtf_map.get(string) / maxfreq) * map.get(string).idf);
                double norms = (file_dftf.get(integer).dftf)*(qtf);
                sum+= ((d * q)  / norms);                       
            }
            ScoreSet set = new ScoreSet(sum, files.get(integer)); 
            file_scores.add(set);
        }    
    }
    public void PrintFileScores()
    {
        Iterator it2;
        Collections.sort(file_scores,new MyComparator());
        it2 = file_scores.iterator();
        gui.jTextArea1.setText("");
        while(it2.hasNext())
        {
            ScoreSet temp = (ScoreSet) it2.next();
            File file = new File(temp.path);
            gui.jTextArea1.setForeground(Color.DARK_GRAY);
            gui.jTextArea1.append(file.getName()+"\n");
            gui.jTextArea1.append(snippet_map.get(temp.path).toLowerCase() +"\n\n");
            //System.out.println((temp.score));
            
        }
        System.out.println("");
    }
    
    public void PrintRssScores()
    {
        Iterator it2;
        Collections.sort(file_scores,new MyComparator());
        it2 = file_scores.iterator();
        gui.jTextArea2.setText("");
        while(it2.hasNext())
        {
            ScoreSet temp = (ScoreSet) it2.next();
            File file = new File(temp.path);
            gui.jTextArea2.setForeground(Color.BLUE);
            gui.jTextArea2.append(file.getName()+"\n");
            gui.jTextArea2.append(item_map.get(temp.path).getTitle()+"\n\n");
        }
    } 
    
    public void ParseQuery(String q) throws FileNotFoundException, IOException, IllegalArgumentException, FeedException
    {
        
        String t = q;                  /////////////////////////////////////////////////prepei na dw mode edw
        files = new HashMap<Integer,String>();
        file_scores = new ArrayList<ScoreSet>();
        qtf_map = new HashMap<String, Integer>();
        snippet_map = new HashMap<String, String>();
        file_dftf = new HashMap<Integer,DocumentInfo>();
        query_map = new HashMap<Integer, HashMap<String, Integer>>();
        item_map = new HashMap<String, SyndEntry>();
        qtf = 0.0;
        maxfreq = 0;

        int flag=0;
        int cc = 0;
        String[] spl = t.split(" ");
        for (int i = 0; i < spl.length; i++)
        {
            if (map.containsKey(spl[i]))
            {
                if(maxfreq==0) maxfreq=1;
                if(qtf_map.containsKey(spl[i]))
                {
                    int x = qtf_map.get(spl[i]);
                    qtf_map.put(spl[i], ++x);
                    if (x>maxfreq) maxfreq=x;
                }
                else
                {
                    qtf_map.put(spl[i], 1);
                }
            }
            else
            {
                cc++;
                if (cc == spl.length)
                {
                    if (mode==0)gui.jTextArea1.setText("No results found.");
                    else gui.jTextArea2.setText("No results found.");
                    flag=1;
                }
            }
        }
        if (flag==0)
        {
            CalculateQnorm();
            for (int j = 0; j < spl.length; j++)
            {
                if (map.containsKey(spl[j]))
                {
                    Seek(spl[j], files);
                }
            }
            CalculateScore();
            if (mode==0) PrintFileScores();
            else PrintRssScores();
        }
    }
    private void Seek(String term, HashMap<Integer,String> file) throws FileNotFoundException, IOException, IllegalArgumentException, FeedException
    {
        RandomAccessFile rafp;
        RandomAccessFile rafd;
        if (mode == 0)
        {
            rafp=new RandomAccessFile("FileCollectionIndex/PostingFile.txt","r");
            rafd=new RandomAccessFile("FileCollectionIndex/DocumentsFile.txt","r");
        }
        else
        {
            rafp=new RandomAccessFile("RssCollectionIndex/PostingFile.txt","r");
            rafd=new RandomAccessFile("RssCollectionIndex/DocumentsFile.txt","r");
        }
        String[] lineparts;
        String[] parts;
        int bytes = map.get(term).bytes;
        int buf = map.get(term).buf;
        rafp.seek(bytes);
        byte[] buffer = new byte[buf - 1];
        rafp.read(buffer);
        String b = new String (buffer);
        lineparts = b.split("\\r\\n");
        int count = lineparts.length;
        for (int i = 0; i < count; i++)
        {
            int partbytes;
            parts = lineparts[i].split(" ");
            int strBytes = Integer.parseInt(parts[3]);
            int strBuf = 30;
            if (i == count -1)
            {
                String str = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
                partbytes = Integer.parseInt(str);
            }
            else
            {
                partbytes = Integer.parseInt(parts[parts.length - 1]);
            }
            int fid = Integer.parseInt(parts[0]);
            int maxtf = Integer.parseInt(parts[1]);
            int tfi = Integer.parseInt(parts[2]);
            if (query_map.get(fid) == null)
            {
                query_map.put(fid, new HashMap<String, Integer>());
            }
            query_map.get(fid).put(term, tfi);
            rafd.seek(partbytes);
            String st = rafd.readLine();
            if (!(st == null))
            {
                String[] str = st.split(" ");
                double dftf = Double.parseDouble(str[1]);
                file_dftf.put(fid, new DocumentInfo());
                file_dftf.get(fid).dftf = dftf;
                file_dftf.get(fid).maxtf = maxtf;
                String string = "";
                for (int j = 2; j < str.length - 1; j++){
                    string = string.concat(str[j] + " ");
                }
                if (mode == 0) 
                {
                    RandomAccessFile raf = new RandomAccessFile(string, "r");
                    raf.seek(strBytes);
                    
                    String bb = raf.readLine();
                    
                    try {                        
                        int start = bb.indexOf(term) - 16;
                        int end = bb.indexOf(term) + 20;
                        String sss = bb.substring(start, end);
                        snippet_map.put(string, "..." + sss + "...");
                    } catch (StringIndexOutOfBoundsException e) {
                        snippet_map.put(string, bb);
                    } catch (NullPointerException e) {
                        snippet_map.put(string, bb);
                    }                    
                } 
                else 
                {
                    BufferedReader reader=new BufferedReader(new FileReader(string));
                    SyndFeed feed = new SyndFeedInput().build(reader);
                    ArrayList<SyndEntry> list = new ArrayList<SyndEntry>(feed.getEntries());
                    String tmp = list.get(strBytes).getTitle()+"\n"+list.get(strBytes).getDescription().getValue();
                    snippet_map.put(string,tmp);
                    item_map.put(string, list.get(strBytes));
                }
                if (!file.containsValue(string)) {
                    file.put(fid, string);
                }
            }          
        }
    }  
}
