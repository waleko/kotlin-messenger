package ru.kotlin566.messenger.android_client

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView


import ru.kotlin566.messenger.android_client.ChatFragment.OnListFragmentInteractionListener
import ru.kotlin566.messenger.android_client.dummy.DummyContent.DummyItem

import kotlinx.android.synthetic.main.fragment_chat.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class MyChatRecyclerViewAdapter(
    private val mValues: List<DummyItem>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MyChatRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as DummyItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // TODO make normal dummy and remove dummy
        val item = mValues[position]
        holder.mDisplayName.text = item.dispayName
        holder.mDateTime.text = item.messageTime
        holder.mMessageText.text = item.messageText
        holder.mProfilePic.setImageDrawable()
//        holder.mIdView.text = item.id
//        holder.mContentView.text = item.content

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
//        val mIdView: TextView = mView.item_number
//        val mContentView: TextView = mView.content
        val mDisplayName: TextView = mView.messageDisplayName
        val mDateTime: TextView = mView.messageDisplayName
        val mMessageText: TextView = mView.messageText
        val mProfilePic: ImageView = mView.messageProfilePicture


        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
