package File_Information_Retrieval;

import com.sun.syndication.io.FeedException;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileIndex
{
    /* Term hash maps*/
    private HashMap<String,HashMap<File,Short>> tf_map = new HashMap<String,HashMap<File,Short>>();
    private HashMap<String, Short> df_map = new HashMap<String, Short>();
    private HashMap<String,HashMap<File,HashSet<Integer>>> positions_map=new HashMap<String,HashMap<File,HashSet<Integer>>>();
    /* File hash maps */
    private HashMap<File,HashMap<String,Short>> file_tfi = new HashMap<File,HashMap<String,Short>>();
    private HashMap<File,FileInfo> file_map = new HashMap<File,FileInfo>();
    /* Helper structures */
    private TreeMap<Short,File> fid_map;
    private HashSet stopSet = new HashSet();
    
    private short fid=1;
    private int pbytes=0;
    private short maxfreq=0;
    
    private void FileFrequencyFinder(File temp) throws IOException
    {
        String line;
        file_tfi.put(temp,new HashMap());
        String delimiter = " \t\n\r\f\\,./?;:'\"“€”{}()[]1234567890-_=+!@#$%^&*`'…﻿–•°<>«»—﻿‘’�ԓӓ";
        BufferedReader reader=readFile(temp.getPath());
        pbytes = 0;
        maxfreq = 0;
        while ((line = reader.readLine()) != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(line, delimiter);
            CalculateFrequencies(tokenizer,temp,line);
        }
        file_map.get(temp).maxfq = maxfreq;
    }
    private void CalculateFrequencies(StringTokenizer tokenizer,File temp,String line)
    {
            HashSet testSet = new HashSet();
            String currentToken = null;
            while(tokenizer.hasMoreTokens())
            {
                currentToken = tokenizer.nextToken().toLowerCase();
                if (!stopSet.contains(currentToken))
                {
                    if(maxfreq==0) maxfreq=1;
                    /*Compute df */
                    if(!testSet.contains(currentToken))
                    {
                        testSet.add(currentToken);
                        if(!df_map.containsKey(currentToken))
                        {
                            tf_map.put(currentToken, new HashMap());
                            positions_map.put(currentToken, new HashMap());
                            positions_map.get(currentToken).put(temp, new HashSet());
                            df_map.put(currentToken ,(short) 1);
                        }
                        else
                        {
                            if (positions_map.get(currentToken).get(temp)==null) positions_map.get(currentToken).put(temp, new HashSet());
                            short x=df_map.get(currentToken);
                            df_map.put(currentToken, ++x);
                        }
                    }
                    /* compute tf */
                    if (!tf_map.get(currentToken).containsKey(temp))
                    {
                        tf_map.get(currentToken).put(temp,(short) 1);
                    }
                    else
                    {
                        short x=tf_map.get(currentToken).get(temp);
                        tf_map.get(currentToken).put(temp, ++x);
                        if (x>maxfreq) maxfreq=x;
                    }
                    if (!file_tfi.get(temp).containsKey(currentToken))
                    {
                        file_tfi.get(temp).put(currentToken,(short) 1);
                    }
                    else
                    {
                        short x=file_tfi.get(temp).get(currentToken);
                        file_tfi.get(temp).put(currentToken, ++x);
                    }
                    positions_map.get(currentToken).get(temp).add(pbytes);
                }
            }
            pbytes+=line.getBytes().length+2;
    }
    public void GenerateFiles(SortedMap<String,Short> sortedmap)
    {
        makeDocumentsFile();
        
        int bytes=0;
        try 
        {     
            FileWriter fstreamV;
            BufferedWriter outV;
            FileWriter fstreamP;
            BufferedWriter outP;
            fstreamV = new FileWriter("FileCollectionIndex/VocabularyFile.txt");
            outV = new BufferedWriter(fstreamV);
            fstreamP = new FileWriter("FileCollectionIndex/PostingFile.txt");
            outP = new BufferedWriter(fstreamP);
            Iterator<String> it = sortedmap.keySet().iterator();
            Iterator it2;
            int oldbytes = 0;
            while (it.hasNext())  //Vocabulary file
            {
                String temp=it.next();
                double df=(double)sortedmap.get(temp);
                double idf = Math.log((file_map.keySet().size()/df))/Math.log(2);
                outV.write(temp + " " + idf);
                HashMap map=tf_map.get(temp);
                it2=map.keySet().iterator();
                outV.write(" "+bytes+" ");
                oldbytes = bytes;
                while(it2.hasNext()) //Posting file
                {
                    File file=(File)it2.next();
                    int f_id=file_map.get(file).fid;
                    int fBytes=file_map.get(file).pointer;
                    int lmaxfreq=file_map.get(file).maxfq;
                    String s=f_id+" "+ lmaxfreq+" "+map.get(file);
                    Iterator it3=positions_map.get(temp).get(file).iterator();
                    while(it3.hasNext()) //Making positions string
                    {
                        Integer positions=(Integer) it3.next();
                        s+=" "+positions;
                    }
                    s+=" "+fBytes+"\r\n";
                    bytes+=s.getBytes().length;
                    outP.write(s);
                }
                int minus = bytes - oldbytes;
                outV.write(minus + "\r\n");
            }
            outV.close();
            outP.close();
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(FileIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void makeDocumentsFile()
    {
        String extension;
        int bytes = 0;
        FileWriter fstream;
        try 
        {
            fstream = new FileWriter("FileCollectionIndex/DocumentsFile.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            fid_map=reverse(file_map);
            Iterator it;
            it=fid_map.keySet().iterator();
            while(it.hasNext())
            {
                Short x=(Short)it.next();
                File temp=(File)fid_map.get(x);
                extension = GetExtension(temp);
                file_map.get(temp).pointer = bytes;
                double vD = Math.sqrt(ComputeVd(temp));
                String s=x+" "+ vD + " " + temp.getAbsolutePath() + " " + extension + "\r\n";
                bytes+=s.getBytes().length;
                out.write(s);
            }
            out.close();
        } 
        catch (IOException ex)
        {
            Logger.getLogger(FileIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public double ComputeVd(File file)
    {
        double lmaxfreq=file_map.get(file).maxfq;
        double n= file_map.keySet().size();
        double dftf=0;
        Iterator<String> i=file_tfi.get(file).keySet().iterator();
        while(i.hasNext())
        {
            String s=i.next();

            double idf = Math.log(n/df_map.get(s))/Math.log(2);
            dftf += Math.pow(((file_tfi.get(file).get(s)/lmaxfreq) * idf),2);
        }
        return dftf;
    }
    public SortedMap<String,Short> FileMainOperations(String path) throws FileNotFoundException, IOException, IllegalArgumentException, FeedException
    {
        File f = new File("FileCollectionIndex");
        f.mkdir();
        File folder = new File(path);
        getFiles(folder);
        Iterator it;
        it=file_map.keySet().iterator();
        while(it.hasNext())
        {
            File temp;
            temp=(File)it.next();
            FileFrequencyFinder(temp);
        }
        stopSet=null;
        SortedMap<String,Short> sortedmap= new TreeMap<String,Short>(df_map); //Used for Vocabulary file
        return sortedmap;
    }
    private void getFiles(File folder) throws IOException
    {
        folder.setReadOnly();
        File[] files = folder.listFiles();
        for(int j=0; j<files.length; j++)
        {
            if (!files[j].isDirectory())
            {
                file_map.put(files[j], new FileInfo());
                file_map.get(files[j]).fid=fid++; // file_map CREATING
            }
            if (files[j].isDirectory()) getFiles(files[j]);
        }
    }
    public void readStopWords(String path) throws FileNotFoundException, IOException
    {
        StringTokenizer tokenizer = null;
        String delimiter = "\t\n\r\f";
        String line = null, currentToken = null;
        BufferedReader reader = readFile(path);
        while ((line = reader.readLine()) != null)
        {
            tokenizer = new StringTokenizer(line, delimiter);
            while(tokenizer.hasMoreTokens())
            {
                currentToken = tokenizer.nextToken();
                stopSet.add(currentToken);
            }
        }
    }
    private BufferedReader readFile(String arguement) throws FileNotFoundException, IOException
    {
        BufferedReader reader = null;
        reader = new BufferedReader(new FileReader(arguement));
        return reader;
    }
    private static  TreeMap<Short,File> reverse(Map<File,FileInfo> map) 
    {
    TreeMap<Short,File> rev = new TreeMap<Short, File>();
    for(Map.Entry<File,FileInfo> entry : map.entrySet())
        rev.put(entry.getValue().fid, entry.getKey());
    return rev;
    }
    private String GetExtension(File temp)
    {
        int dotPos;
        String fileName,extension;
        fileName = temp.toString();
        fileName = fileName.trim();
        dotPos = fileName.lastIndexOf(".");
        extension = fileName.substring(dotPos + 1);
        return extension.toLowerCase();
    }
}
