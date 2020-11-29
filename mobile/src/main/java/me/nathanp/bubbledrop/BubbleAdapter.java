package me.nathanp.bubbledrop;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

// Adapter for the recycler view in CommentFeedActivity. You do not need to modify this file
public class BubbleAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private ArrayList<Bubble> mBubbles;
    private View.OnClickListener mBubbleClickListner;

    BubbleAdapter(Context context, ArrayList<Bubble> bubbles, View.OnClickListener bubbleClickListener) {
        mContext = context;
        mBubbles = bubbles;
        mBubbleClickListner = bubbleClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // here, we specify what kind of view each cell should have. In our case, all of them will have a view
        // made from comment_cell_layout
        View view = LayoutInflater.from(mContext).inflate(R.layout.bubble_cell_layout, parent, false);
        return new BubbleViewHolder(view);
    }


    // - get element from your dataset at this position
    // - replace the contents of the view with that element
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // here, we the comment that should be displayed at index `position` in our recylcer view
        // everytime the recycler view is refreshed, this method is called getItemCount() times (because
        // it needs to recreate every cell).
        Bubble comment = mBubbles.get(position);
        ((BubbleViewHolder) holder).bind(comment);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mBubbles.size();
    }
}

class BubbleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    // each data item is just a string in this case
    private TextView mTypeTextView;
    private TextView mDateTextView;
    private Context mContext;
    private Bubble bubble;
    private View itemView;

    //Firebase things
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    BubbleViewHolder(View itemView) {
        super(itemView);
        this.itemView = itemView;
        mContext = (BubblesActivity) itemView.getContext();
        RelativeLayout commentBubbleLayout = itemView.findViewById(R.id.bubble_cell_layout);
        mTypeTextView = commentBubbleLayout.findViewById(R.id.type_text_view);
        mDateTextView = commentBubbleLayout.findViewById(R.id.time_text_view);
    }

    @Override
    public void onClick (View v) {
        Log.d("BubbleViewHolder", "onClick");
//
//        BubblesActivity my_bubbles = (BubblesActivity) mContext;
//
////        DatabaseReference bubbleRef = database.getReference("bubbles").child(key);
//        my_bubbles.showBubblePopup(bubble.bubbleRef);

//        mContext.expandBubbleItem(v);
    }

    void bind(Bubble bubble) {
        this.bubble = bubble;
        switch (bubble.bubbleType) {
            case Bubble.TEXT_BUBBLE: {
                mTypeTextView.setText("Text");
                break;
            }
            case Bubble.AUDIO_BUBBLE: {
                mTypeTextView.setText("Audio");
                break;
            }
            case Bubble.PICTURE_BUBBLE: {
                mTypeTextView.setText("Image");
                break;
            }
        }
        mDateTextView.setText(mContext.getString(R.string.time_ago, bubble.elapsedTimeString()));

        this.itemView.setTag(bubble.key);
    }
}
