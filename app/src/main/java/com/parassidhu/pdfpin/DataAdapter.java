package com.parassidhu.pdfpin;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {
    private ArrayList<ListItem> listItems;
    private Context context;
    int i =0;

    public DataAdapter(Context context,ArrayList<ListItem> listItems) {
        this.context = context;
        this.listItems = listItems;
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        viewHolder.filename.setText(listItems.get(i).getName());
        viewHolder.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new LovelyTextInputDialog(context)
                        .setTopColorRes(R.color.blue)
                        .setTitle("Rename")
                        .setInitialInput(viewHolder.filename.getText().toString())
                        .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                            @Override
                            public void onTextInputConfirmed(String text) {
                                if(!text.isEmpty()) {
                                    listItems.get(viewHolder.getAdapterPosition()).setName(text);
                                    viewHolder.filename.setText(listItems.get(viewHolder.getAdapterPosition()).getName());
                                }
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView filename;
        ImageButton edit;
        public ViewHolder(View view) {
            super(view);
            filename = (TextView)view.findViewById(R.id.filename);
            edit = (ImageButton) view.findViewById(R.id.edit);
        }
    }
}