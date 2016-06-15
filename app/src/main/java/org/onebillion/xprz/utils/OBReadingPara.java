package org.onebillion.xprz.utils;

import android.graphics.PointF;

import java.util.List;

/**
 * Created by alan on 02/06/16.
 */
public class OBReadingPara
{
    public PointF position;
    public List<OBReadingWord> words;
    public String text;
    public int paraNo;

    public OBReadingPara(String sentence,int n)
    {
        words = OBReadingWord.WordsFromString(sentence,n);
        buildText();
        paraNo = n;
    }

    public void buildText()
    {
        StringBuilder mstr = new StringBuilder();
        for (OBReadingWord w : words)
        {
            w.index = mstr.length();
            mstr.append(w.text);
        }
        text = mstr.toString();
    }

}
