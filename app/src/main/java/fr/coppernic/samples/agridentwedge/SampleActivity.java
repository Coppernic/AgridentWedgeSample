package fr.coppernic.samples.agridentwedge;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.coppernic.sdk.agrident.Parameters;

public class SampleActivity extends AppCompatActivity {

    private TextView mTextMessage;
    private List<Parameters> parametersList = new ArrayList<>();
    private ParameterAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        mTextMessage = findViewById(R.id.message);
        recyclerView = findViewById(R.id.recyclerParameter);

        adapter = new ParameterAdapter(parametersList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        initParameterData();
    }

    private void initParameterData(){
        parametersList.add(new Parameters(Parameters.BAUDRATE));
        parametersList.add(new Parameters(Parameters.DELAYTIME));
        parametersList.add(new Parameters(Parameters.OUTPUT_FORMAT));
        parametersList.add(new Parameters(Parameters.TAG_TYPE));
        parametersList.add(new Parameters(Parameters.TIMING));

        adapter.notifyDataSetChanged();
    }

}
