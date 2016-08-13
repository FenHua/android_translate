package com.example.yan.tramslate;

import android.content.res.Resources;
import android.os.Handler;
import android.support.v4.widget.Space;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public class Translate extends AppCompatActivity
{
    private Spinner fromSpinner;
    private Spinner toSpinner;
    private EditText origText;
    private TextView transText;
    private TextView retransText;
    private TextWatcher textWatcher;
    private AdapterView.OnItemSelectedListener itemListener;
    private Handler guiThread;
    private ExecutorService transThread;
    private Runnable updateTask;
    private Future transPending;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        initThreading();
        findViews();
        setAdapters();
        setListeners();
    }
    //Get a handle to all user interface elements
    private void findViews()
    {
        fromSpinner=(Spinner)findViewById(R.id.from_language);
        toSpinner=(Spinner)findViewById(R.id.to_language);
        origText=(EditText)findViewById(R.id.original_text);
        transText = (TextView)findViewById(R.id.translated_text);
        retransText=(TextView)findViewById(R.id.retranslated_text);
    }
    //Define data source for the spinners
    private void setAdapters()
    {
        //Spinner list comes from a resource
        //Spinner user interface uses standard layouts
        ArrayAdapter<CharSequence>adapter=ArrayAdapter.createFromResource(this,R.array.languages,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResourc(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);
        //Automatically select two spinner items
        fromSpinner.setSelection(0);
        toSpinner.setSelection(2);
    }
    //Setup user interface event handlers
    private void setListeners()
    {
        //Define event listeners
        textWatcher=new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queueUpdate(1000);//millisecond
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        itemListener=new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                queueUpdate(200);//millisecond
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };
        //Set listener on graphical user interface widgets
        origText.addTextChangedListener(textWatcher);
        fromSpinner.setOnItemSelectedListener(itemListener);
        toSpinner.setOnItemSelectedListener(itemListener);
    }
    /*
    Initialize multi_threading.There are two threads:1.the main
    graphical user interfdce thread already start by android,and 2.
    The translate thread,which we start using an executor.
     */
    private void initThreading()
    {
        guiThread=new Handler();
        transThread= Executors.newSingleThreadExecutor();
        //This task does a translation and update the screen
        updateTask=new Runnable() {
            @Override
            public void run() {
                //Get tetx to translate
                String original=origText.getText().toString().trim();
                //cancel previous translation if there was one
                if(transPending!=null)
                {
                    transPending.cancel(true);
                }
                //Take care of the easy case
                if(original.length()==0)
                {
                    transText.setText(R.string.empty);
                }
                else
                {
                    //Let user know we are doing something
                    transText.setText(R.string.translating);
                    retransText.setText(R.string.translating);
                    //Begin translation now but do not wait
                    try
                    {
                        TranslateTask translateTask=new TranslateTask(Translate.this,original,getLang(fromSpinner),getLang(toSpinner));
                        transPending=transThread.submit(translateTask);
                    }
                    catch (RejectedExecutionException e)
                    {

                        //Unable to start new task
                        transText.setText(R.string.translation_error);
                        retransText.setText(R.string.translation_error);
                    }
                }
            }
        };
    }
    /*
    Extract the language code from the current spinner item
     */
    private String getLang(Spinner spinner)
    {
        String result=spinner.getSelectedItem().toString();
        int lparen=result.indexOf('(');
        int rparen=result.indexOf(')');
        result=result.substring(lparen+1,rparen);
        return result;
    }
    /*
    Request an update to start after a short delay
     */
    private void queueUpdate(long delayMillis)
    {
        //Cancel previous update if it hasn't started yet
        guiThread.removeCallbacks(updateTask);
        //Start an update if nothing happens after a few milliseconds
        guiThread.postDelayed(updateTask,delayMillis);
    }
    //Modify text on the screen(call from another thread)
    public void setTranslated(String text)
    {
        guiSetText(transText,text);
    }
    //Modify text on the screen
    public void setReTranslated(String text)
    {
        guiSetText(transText,text);
    }
    //All changes to the Gui must be done in the gui thread
    private void guiSetText(final TextView view,final String text)
    {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                view.setText(text);
            }
        })
    }
}






















