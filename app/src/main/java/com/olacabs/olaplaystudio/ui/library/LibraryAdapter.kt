package com.olacabs.olaplaystudio.ui.library

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.olacabs.olaplaystudio.R
import com.olacabs.olaplaystudio.data.model.MediaDetail
import com.olacabs.olaplaystudio.utils.visible
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_media.view.*
import javax.inject.Inject

class LibraryAdapter @Inject
constructor() : RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder>() {

    @Inject lateinit var mPicasso: Picasso

    val mMediaList: ArrayList<MediaDetail> = arrayListOf()
    var mClickListener: ClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_media, parent, false)
        return LibraryViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        val media = mMediaList[position]
        val view = holder.itemView

        view.media_title.text = media.song ?: ""
        view.media_artist.text = media.artists ?: ""

        media.cover_image?.let {
            mPicasso.load(it).placeholder(R.drawable.album_placeholder).into(view.media_image)
        }

        view.play_button.setOnClickListener {
            mClickListener?.onMediaClick(mediaDetail = media)
        }

        val drawable = getDrawableByState(view.context.applicationContext, media.state)
        if (drawable != null) {
            view.play_button?.let {
                it.setImageDrawable(drawable)
                (it.drawable as? AnimationDrawable)?.start()
            }
        } else {
            view.play_button?.setImageResource(R.drawable.ic_media_play)
        }

        view.fav_iv.setOnClickListener { mClickListener?.onFavClick(holder.adapterPosition, media) }
        view.download_iv.setOnClickListener { mClickListener?.onDownloadClick(media) }
        view.fav_iv.setImageResource(
                if (media.fav) R.drawable.ic_media_favorite_fill
                else R.drawable.ic_media_favorite_border)

        view?.download_iv?.visible(!media.isDownloaded)

    }

    fun setClickListener(clickListener: ClickListener) {
        mClickListener = clickListener
    }

    override fun getItemCount(): Int {
        return mMediaList.size
    }

    interface ClickListener {
        fun onMediaClick(mediaDetail: MediaDetail)
        fun onFavClick(position: Int, mediaDetail: MediaDetail)
        fun onDownloadClick(mediaDetail: MediaDetail)
    }

    class LibraryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {

        private var sColorStatePlaying: Int? = null
        private var sColorStateNotPlaying: Int? = null

        fun getDrawableByState(context: Context?, state: Int): Drawable? {
            if (context == null)
                return null

            if (sColorStateNotPlaying == null || sColorStatePlaying == null)
                initializeColorStateLists(context)

            when (state) {
                PlaybackStateCompat.STATE_NONE -> {
                    val pauseDrawable = ContextCompat.getDrawable(context,
                            R.drawable.ic_media_play)
                    setDrawableTint(pauseDrawable, sColorStateNotPlaying!!)
                    return pauseDrawable
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    val animation = ContextCompat.getDrawable(context, R.drawable.ic_equalizer_anim)
                            as AnimationDrawable
                    setDrawableTint(animation, sColorStatePlaying!!)
                    //Starting animation here was not working in some device
                    return animation
                }
                PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_PAUSED -> {
                    val playDrawable = ContextCompat.getDrawable(context,
                            R.drawable.ic_equalizer1)
                    setDrawableTint(playDrawable, sColorStatePlaying!!)
                    return playDrawable
                }
                else -> return null
            }
        }

        private fun setDrawableTint(drawable: Drawable, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                DrawableCompat.setTint(drawable, color)
            else
                drawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }

        private fun initializeColorStateLists(ctx: Context) {
            sColorStateNotPlaying = ContextCompat.getColor(ctx,
                    R.color.media_item_icon_not_playing)
            sColorStatePlaying = ContextCompat.getColor(ctx,
                    R.color.media_item_icon_playing)
        }
    }
}
