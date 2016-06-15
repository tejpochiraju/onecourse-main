package org.onebillion.xprz.mainui;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.view.View;

import org.onebillion.xprz.controls.OBControl;
import org.onebillion.xprz.controls.OBGroup;
import org.onebillion.xprz.controls.OBLabel;
import org.onebillion.xprz.controls.OBPath;
import org.onebillion.xprz.controls.XPRZ_Presenter;
import org.onebillion.xprz.utils.OBAnim;
import org.onebillion.xprz.utils.OBAnimBlock;
import org.onebillion.xprz.utils.OBAnimationGroup;
import org.onebillion.xprz.utils.OBReadingPara;
import org.onebillion.xprz.utils.OBReadingWord;
import org.onebillion.xprz.utils.OB_Maths;
import org.onebillion.xprz.utils.OB_utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alan on 10/06/16.
 */
public class X_ReadingIRead extends X_Reading
{
    static final int STATUS_DOING_WORD = 1001;
    XPRZ_Presenter presenter;
    OBControl wordback,wordback2;
    List<OBReadingWord> words;
    RectF saveWordBackFrame = new RectF();
    int lineColour,boxColour,borderColour;
    boolean animatingBack,wordHelpAvailable;


    public void start()
    {
        setStatus(0);
        new AsyncTask<Void, Void,Void>()
        {
            protected Void doInBackground(Void... params) {
                try
                {
                    if (pageNo == 0)
                    {
                        setStatus(STATUS_DOING_DEMO);
                        demoa();
                    }
                    setStatus(STATUS_AWAITING_CLICK);
                    waitForSecs(4.5);
                }
                catch (Exception exception)
                {
                }
                showNextArrow(true);
                return null;
            }}.execute();
    }

    public int buttonFlags()
    {
        int flags = 0;
        flags |= MainViewController().SHOW_TOP_LEFT_BUTTON;
        if (pageNo > 0)
            flags |= MainViewController().SHOW_BOTTOM_LEFT_BUTTON;
        return flags;
    }

    boolean wordHelpAvailable()
    {
        String localPath = getLocalPath("book.xml");
        if (localPath != null)
        {
            String dirPath = OB_utils.stringByDeletingLastPathComponent(localPath);
            String slowPath = OB_utils.stringByAppendingPathComponent(dirPath,String.format("ps%d_%d.etpa",pageNo,1));
            if (OB_utils.fileExistsAtPath(slowPath))
                return true;
            List<String> files = OB_utils.filesAtPath(dirPath);
            String prefix = String.format("psyl%d_.*",pageNo);
            Pattern p = Pattern.compile(prefix);
            for (String s : files)
            {
                Matcher matcher = p.matcher(s);
                matcher.find();
                if (matcher.matches())
                    return true;
            }
        }
        return false;
    }

    public void setUpScene()
    {
        wordHelpAvailable = wordHelpAvailable();
        super.setUpScene();

        //textBox.setBackgroundColor(Color.YELLOW);

        OBPath wb = (OBPath) objectDict.get("wordback");
        wordback = new OBControl();
        wordback.setFrame(wb.frame);
        wordback.backgroundColor = boxColour = wb.fillColor();
        wordback.setBorderColor(wb.strokeColor());
        wordback.borderWidth = wb.lineWidth();
        wordback.cornerRadius = applyGraphicScale(3);

        wordback2 = wordback.copy();
        wordback2.backgroundColor = borderColour = Color.WHITE;
        wordback2.setBorderColor(0);
        wordback2.borderWidth = 0;
        textBox.insertMember(wordback,0,"wordback");
        textBox.insertMember(wordback2,0,"wordback2");
        wb.hide();
        wordback.hide();
        wordback2.hide();
        List<OBReadingWord> wds = new ArrayList<>();
        for (OBReadingPara para : paragraphs)
            for (OBReadingWord rw : para.words)
                if ((rw.flags & OBReadingWord.WORD_SPEAKABLE) != 0)
                    wds.add(rw);
        words = wds;
        if (pageNo == 0 && words.size() > 0)
        {
            OBLabel lab = words.get(0).label;
            RectF f = convertRectFromControl(lab.frame,textBox);
            jumpOffset = f.height() * 0.6f;
            if (f.top - jumpOffset < 2)
            {
                jumpOffset = f.top - 2;
            }
        }
        textBox.setShouldTexturise(false);
    }

    public void demoa() throws Exception {
        lockScreen();
        loadEvent("anna");
        presenter = XPRZ_Presenter.characterWithGroup((OBGroup)objectDict.get("presenter"));
        presenter.control.setZPosition(200);
        presenter.control.setProperty("restpos",new PointF(presenter.control.position().x,presenter.control.position().y));
        presenter.control.setRight(0);
        presenter.control.show();
        unlockScreen();

        presenter.walk((PointF) presenter.control.propertyValue("restpos"));
        presenter.faceFront();
        waitForSecs(0.2f);
        Map<String,List> eventd = (Map<String, List>) audioScenes.get("a");

        List<Object> aud = eventd.get("DEMO");
        presenter.speak(aud.subList(0,1),this);
        waitForSecs(0.3f);

        if (wordHelpAvailable)
        {
            presenter.speak(aud.subList(1,2),this);
            presenter.moveHandfromIndex(1,6,0.4);
            waitForSecs(0.4f);
            presenter.moveHandfromIndex(6,0,0.3);
        }
        else
            presenter.speak(aud.subList(2,3),this);
        waitForSecs(1f);

        PointF currPos = presenter.control.position();
        PointF destpos = new PointF(-presenter.control.width()/2, currPos.y);
        setStatus(STATUS_AWAITING_CLICK);
        presenter.walk(destpos);
        waitForSecs(3);
        showNextArrow(true);
    }

    public void setUpDecorationForWord(OBReadingWord rw)
    {
        if (rw == null)
        {
            wordback.hide();
            return;
        }
        float px1 = applyGraphicScale(1);
        RectF f = new RectF(rw.label.frame());
        f.inset(-px1,-px1);
        wordback.setFrame(f);
        saveWordBackFrame = new RectF(wordback.frame());
        float amt = 1 * px1;
        f = new RectF(wordback.frame());
        f.inset(-16,-16);
        wordback2.setFrame(f);
        wordback.setZPosition(LABEL_ZPOS - 1);
        wordback2.setZPosition(LABEL_ZPOS - 2);
    }

    public void highlightAndSpeakWord(OBReadingWord w) throws Exception
    {
        boolean withBackground = jumpOffset > 0;
        highlightWord(w,true,withBackground);

        speakWordAsPartial(w);
        lowlightWord(w,true,withBackground);
    }

    public void doWord(OBReadingWord w)
    {
        try
        {
            long token = takeSequenceLockInterrupt(true);
            if (token == sequenceToken)
            {
                setUpDecorationForWord(w);
                lockScreen();
                wordback.show();
                //wordback2.show();
                unlockScreen();
                highlightAndSpeakWord(w);
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
        lockScreen();
        wordback.hide();
        wordback2.hide();
        w.label.setPosition(w.homePosition);
        w.label.setZPosition(LABEL_ZPOS);
        wordback.setZPosition(LABEL_ZPOS-1);
        wordback2.setZPosition(LABEL_ZPOS-2);
        unlockScreen();
        sequenceLock.unlock();
    }

    public OBReadingWord lastWordOnPage()
    {
        OBReadingPara p = paragraphs.get(paragraphs.size()-1);
        int idx = p.words.size() - 1;
        while (idx >= 0 && (p.words.get(idx).flags&OBReadingWord.WORD_SPEAKABLE) == 0)
            idx--;
        if (idx < 0)
            return null;
        return p.words.get(idx);
    }

    public void highlightWord(OBReadingWord w,boolean withBackground,boolean jump)
    {
        lockScreen();
        w.label.setColour(highlightColour);
        w.label.setZPosition(LABEL_HI_ZPOS);
        if (withBackground)
        {
            wordback.show();
            wordback.setZPosition(LABEL_HI_ZPOS - 1);
            //wordback2.show();
            wordback2.setZPosition(LABEL_HI_ZPOS - 2);
        }
        unlockScreen();
        if (jump)
        {
            OBAnimationGroup agp = new OBAnimationGroup();
            PointF pos = OB_Maths.OffsetPoint(w.label.position(), 0, -jumpOffset);
            OBAnim anim1 = OBAnim.moveAnim(pos,w.label);
            final float top = saveWordBackFrame.top;
            final float top2 = top - jumpOffset;
            float bot = saveWordBackFrame.bottom;
            final float amt = applyGraphicScale(1);
            OBAnim anim2 = new OBAnimBlock()
            {
                @Override
                public void runAnimBlock(float frac)
                {
                    RectF f = new RectF(saveWordBackFrame);
                    float y = OB_Maths.interpolateVal(top,top2,frac);
                    f.top = y;
                    wordback.setFrame(f);
                    f.inset(-amt,-amt);
                    wordback2.setFrame(f);
                }
            };
            agp.applyAnimations(Arrays.asList(anim1,anim2),0.1,true,OBAnim.ANIM_EASE_IN_EASE_OUT,this);
        }
    }

    public void lowlightWord(OBReadingWord w,boolean withBackground,boolean jump)
    {
        if (jump)
        {
            OBAnimationGroup agp = new OBAnimationGroup();
            PointF pos = OB_Maths.OffsetPoint(w.label.position(), 0, jumpOffset);
            OBAnim anim1 = OBAnim.moveAnim(pos,w.label);
            final float top = saveWordBackFrame.top;
            final float top2 = top - jumpOffset;
            final float amt = applyGraphicScale(1);
            OBAnim anim2 = new OBAnimBlock()
            {
                @Override
                public void runAnimBlock(float frac)
                {
                    RectF f = new RectF(saveWordBackFrame);
                    float y = OB_Maths.interpolateVal(top2,top,frac);
                    f.top = y;
                    wordback.setFrame(f);
                    f.inset(-amt,-amt);
                    wordback2.setFrame(f);
                }
            };
            agp.applyAnimations(Arrays.asList(anim1,anim2),0.1,true,OBAnim.ANIM_EASE_IN_EASE_OUT,this);
        }
        w.label.setColour(Color.BLACK);
        w.label.setZPosition(LABEL_ZPOS);
        if (withBackground)
        {
            wordback.setZPosition(LABEL_ZPOS - 1);
            wordback2.setZPosition(LABEL_ZPOS - 2);
        }
    }

    public void touchUpAtPoint(PointF pt,View v)
    {
        if (status() == STATUS_DOING_WORD)
        {
            highlightedWord = null;
            setStatus(STATUS_AWAITING_CLICK);
        }
    }

    public void touchMovedToPoint(PointF pt,View v)
    {
        if (status() == STATUS_DOING_WORD)
        {
            Object obj = findTarget(pt);
            if (obj instanceof OBReadingWord)
            {
                final OBReadingWord w = (OBReadingWord) obj;
                if (w != highlightedWord)
                {
                    highlightedWord = w;
                    if (w != null)
                        new AsyncTask<Void, Void, Void>()
                        {
                            @Override
                            protected Void doInBackground(Void... params)
                            {
                                doWord(w);
                                return null;
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                }
            }
        }
    }

    public Object findTarget(PointF pt)
    {
        List<OBControl> labels = new ArrayList<>();
        for (OBReadingWord w : words)
            labels.add(w.label);
        OBControl l = finger(-1,2,labels,pt);
        if (l != null)
            return words.get(labels.indexOf(l));
        return null;
    }

    public void touchDownAtPoint(PointF pt,View v)
    {
        if (status() == STATUS_AWAITING_CLICK && wordHelpAvailable)
        {
            Object obj = findTarget(pt);
            if (obj instanceof OBReadingWord)
            {
                setStatus(STATUS_DOING_WORD);
                final OBReadingWord w = (OBReadingWord) obj;
                highlightedWord = w;
                new AsyncTask<Void, Void, Void>()
                {
                    @Override
                    protected Void doInBackground(Void... params)
                    {
                        doWord(w);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
            else
            {
                setStatus(STATUS_DOING_WORD);
                highlightedWord = null;
            }
        }
    }

}
