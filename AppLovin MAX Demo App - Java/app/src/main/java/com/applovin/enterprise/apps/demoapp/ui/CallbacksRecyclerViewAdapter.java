package com.applovin.enterprise.apps.demoapp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.applovin.enterprise.apps.demoapp.R;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} for the callback RecyclerView in ad activities.
 * <p>
 * Created by Harry Arakkal on 2019-10-21.
 */
public class CallbacksRecyclerViewAdapter
        extends RecyclerView.Adapter<CallbacksRecyclerViewAdapter.ViewHolder>
{
    private final List<String>   callbacks;
    private final LayoutInflater layoutInflater;

    CallbacksRecyclerViewAdapter(final List<String> callbacks, final Context context)
    {
        this.callbacks = callbacks;
        this.layoutInflater = LayoutInflater.from( context );
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType)
    {
        return new ViewHolder( layoutInflater.inflate( R.layout.ad_callback_item, parent, false ) );
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
    {
        holder.callbackName.setText( callbacks.get( position ) );
    }

    @Override
    public int getItemCount()
    {
        return callbacks.size();
    }

    class ViewHolder
            extends RecyclerView.ViewHolder
    {
        private final TextView callbackName;

        ViewHolder(View view)
        {
            super( view );
            callbackName = view.findViewById( R.id.callbackName );
        }

    }
}
