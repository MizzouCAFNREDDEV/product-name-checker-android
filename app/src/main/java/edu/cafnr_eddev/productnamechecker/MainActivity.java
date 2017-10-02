package edu.cafnr_eddev.productnamechecker;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client;
    EditText productName;
    EditText printName;
    Button viewPDF;
    TextView result;
    ProgressBar pbResults;

    static int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 406;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PDFBoxResourceLoader.init(this.getApplicationContext());
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        }

        client = new OkHttpClient();
        productName = (EditText)findViewById(R.id.etProductName);
        printName = (EditText)findViewById(R.id.etPrintName);
        viewPDF = (Button)findViewById(R.id.bViewPDF);
        result = (TextView)findViewById(R.id.tvResult);
        pbResults = (ProgressBar)findViewById(R.id.pbResult);
    }

    public void checkProductName(View view){
        String productNameString = productName.getText().toString();
        result.setVisibility(View.INVISIBLE);
        viewPDF.setVisibility(View.INVISIBLE);
        CheckProductTask task = new CheckProductTask();
        task.execute("http://cafnr-eddev.dev/products?name=" + productNameString);
    }

    public void viewPDF(View view) throws IOException {
        SimpleDateFormat df = new SimpleDateFormat("MMMM dd, yyyy KK:hh a");
        Date d = new Date();
        CreatePDFTask pdfTask = new CreatePDFTask();
        pdfTask.execute(printName.getText().toString(), productName.getText().toString(), df.format(d));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 406: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }



    private class CheckProductTask extends AsyncTask<String, Void, Integer>{

        int run(String url) throws IOException {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.code();
        }

        @Override
        protected Integer doInBackground(String... params) {
            try {
                Log.d("CheckProductTask:", params[0]);
                return run(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return HttpURLConnection.HTTP_OK;

        }

        @Override
        protected void onPostExecute(Integer statusCode) {
            if(statusCode == HttpURLConnection.HTTP_OK){
                result.setText("Approved!");
                result.setVisibility(View.VISIBLE);
                viewPDF.setVisibility(View.VISIBLE);
            }else if(statusCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE){
                result.setText("Not Approved. Please try again.");
                result.setVisibility(View.VISIBLE);
            }else{
                Log.e("HTTP error", "" + statusCode);
            }
        }
    }

    private class CreatePDFTask extends AsyncTask<String, Void, String>{

        PDDocument doc;

        @Override
        protected void onPreExecute() {
            pbResults.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            File documents = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS);
            String dirName = documents.getAbsolutePath() + "/edu.cafnr-eddev.product-checker/";
            File dir = new File(dirName);
            if(!dir.exists()){
                Boolean res = dir.mkdirs();
                Log.d("downloads", res + "");
            }
            try (PDDocument doc = new PDDocument()){
                PDPage page = new PDPage();
                doc.addPage(page);

                PDFont font = PDType1Font.HELVETICA_BOLD;

                try (PDPageContentStream contents = new PDPageContentStream(doc, page)){
                    contents.beginText();
                    contents.setFont(font, 12);
                    contents.newLineAtOffset(100, 700);
                    contents.showText("Name: " + params[0]);
                    contents.newLineAtOffset(0, -50);
                    contents.showText(params[1] + " has been checked and is approved");
                    contents.newLineAtOffset(0, -50);
                    contents.showText(params[2]);
                    contents.endText();
                    contents.close();
                }

                doc.save(dirName + "product-check.pdf");
                return dirName;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String dirName) {
            pbResults.setVisibility(View.INVISIBLE);
            File file = new File(dirName + "product-check.pdf");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            Intent intent1 = Intent.createChooser(intent, "Open With");

            startActivity(intent1);
        }
    }
}
