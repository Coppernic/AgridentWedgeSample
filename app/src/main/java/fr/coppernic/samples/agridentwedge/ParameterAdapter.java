package fr.coppernic.samples.agridentwedge;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import fr.coppernic.sdk.agrident.Parameters;

public class ParameterAdapter extends RecyclerView.Adapter<ParameterAdapter.MyViewHolder> {

    private List<Parameters> parametersList;

    ParameterAdapter(List<Parameters> parametersList){
        this.parametersList = parametersList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Parameters parameter = parametersList.get(position);
        holder.name.setText(parameter.getAddress());
        holder.name.setText(parameter.getValue());
    }

    @Override
    public int getItemCount() {
        return parametersList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name, value;

        MyViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.tvSettingsName);
            value = view.findViewById(R.id.tvSettingsValue);

        }
    }
}
