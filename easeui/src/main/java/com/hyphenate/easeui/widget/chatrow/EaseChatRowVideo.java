package com.hyphenate.easeui.widget.chatrow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMFileMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVideoMessageBody;
import com.hyphenate.easeui.R;
import com.hyphenate.easeui.utils.EaseCommonUtils;
import com.hyphenate.easeui.utils.EaseImageCache;
import com.hyphenate.easeui.utils.EaseImageUtils;
import com.hyphenate.util.DateUtils;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.ImageUtils;
import com.hyphenate.util.TextFormater;
import com.hyphenate.util.UriUtils;

import java.io.File;
import java.io.IOException;

public class EaseChatRowVideo extends EaseChatRowFile {
    private static final String TAG = EaseChatRowVideo.class.getSimpleName();

    private ImageView imageView;
    private TextView sizeView;
    private TextView timeLengthView;
    private ImageView playView;
    private int maxWidth;
    private int maxHeight;

    public EaseChatRowVideo(Context context, boolean isSender) {
        super(context, isSender);
        getScreenInfo(context);
    }

    public EaseChatRowVideo(Context context, EMMessage message, int position, Object adapter) {
        super(context, message, position, adapter);
        getScreenInfo(context);
    }

	@Override
	protected void onInflateView() {
		inflater.inflate(!isSender ? R.layout.ease_row_received_video
                : R.layout.ease_row_sent_video, this);
	}

	@Override
	protected void onFindViewById() {
	    imageView = ((ImageView) findViewById(R.id.chatting_content_iv));
        sizeView = (TextView) findViewById(R.id.chatting_size_iv);
        timeLengthView = (TextView) findViewById(R.id.chatting_length_iv);
        playView = (ImageView) findViewById(R.id.chatting_status_btn);
        percentageView = (TextView) findViewById(R.id.percentage);
	}

	@Override
	protected void onSetUpView() {
        EMVideoMessageBody videoBody = (EMVideoMessageBody) message.getBody();
        String localThumb = videoBody.getLocalThumb();

        if (localThumb != null) {

            showVideoThumbView(localThumb, imageView, videoBody.getThumbnailUrl(), message);
        }
        if (videoBody.getDuration() > 0) {
            String time = DateUtils.toTime(videoBody.getDuration());
            timeLengthView.setText(time);
        }

        if (message.direct() == EMMessage.Direct.RECEIVE) {
            if (videoBody.getVideoFileLength() > 0) {
                String size = TextFormater.getDataSize(videoBody.getVideoFileLength());
                sizeView.setText(size);
            }
        } else {
            long videoFileLength = videoBody.getVideoFileLength();
            sizeView.setText(TextFormater.getDataSize(videoFileLength));
//            if (videoBody.getLocalUrl() != null && new File(videoBody.getLocalUrl()).exists()) {
//                String size = TextFormater.getDataSize(new File(videoBody.getLocalUrl()).length());
//                sizeView.setText(size);
//            }
        }

        EMLog.d(TAG,  "video thumbnailStatus:" + videoBody.thumbnailDownloadStatus());
        if (message.direct() == EMMessage.Direct.RECEIVE) {
            if (videoBody.thumbnailDownloadStatus() == EMFileMessageBody.EMDownloadStatus.DOWNLOADING ||
                    videoBody.thumbnailDownloadStatus() == EMFileMessageBody.EMDownloadStatus.PENDING) {
                imageView.setImageResource(R.drawable.ease_default_image);
            } else {
                // System.err.println("!!!! not back receive, show image directly");
                imageView.setImageResource(R.drawable.ease_default_image);
                if (localThumb != null) {
                    showVideoThumbView(localThumb, imageView, videoBody.getThumbnailUrl(), message);
                }
            }
            return;
        }else{
            if (videoBody.thumbnailDownloadStatus() == EMFileMessageBody.EMDownloadStatus.DOWNLOADING ||
                    videoBody.thumbnailDownloadStatus() == EMFileMessageBody.EMDownloadStatus.PENDING ||
                    videoBody.thumbnailDownloadStatus() == EMFileMessageBody.EMDownloadStatus.FAILED) {
                progressBar.setVisibility(View.INVISIBLE);
                percentageView.setVisibility(View.INVISIBLE);
                imageView.setImageResource(R.drawable.ease_default_image);
            } else {
                progressBar.setVisibility(View.GONE);
                percentageView.setVisibility(View.GONE);
                imageView.setImageResource(R.drawable.ease_default_image);
                showVideoThumbView(localThumb, imageView, videoBody.getThumbnailUrl(), message);
            }
        }
	}

    /**
     * show video thumbnails
     * 
     * @param localThumb
     *            local path for thumbnail
     * @param iv
     * @param thumbnailUrl
     *            Url on server for thumbnails
     * @param message
     */
    @SuppressLint("StaticFieldLeak")
    private void showVideoThumbView(final String localThumb, final ImageView iv, String thumbnailUrl, final EMMessage message) {
        // first check if the thumbnail image already loaded into cache
        EMLog.d(EMClient.TAG, " localThumb = "+localThumb);
        Bitmap bitmap = EaseImageCache.getInstance().get(localThumb);
        if (bitmap != null) {
            // thumbnail image is already loaded, reuse the drawable
            ViewGroup.LayoutParams params = EaseImageUtils.showImage(iv, bitmap, maxWidth, maxHeight);
            setBubbleView(params.width, params.height);
        } else {
            imageView.setImageResource(R.drawable.ease_default_image);
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground(Void... params) {
                    if(!UriUtils.isFileExistByUri(context, UriUtils.getLocalUriFromString(localThumb))) {
                        return null;
                    }
                    String filePath = UriUtils.getFilePath(context, localThumb);
                    if(!TextUtils.isEmpty(filePath)) {
                        if (new File(filePath).exists()) {
                            return ImageUtils.decodeScaleImage(filePath, maxWidth, maxHeight);
                        } else {
                            return null;
                        }
                    }else {
                        if(!TextUtils.isEmpty(localThumb) && localThumb.startsWith("content")) {
                            if(UriUtils.isFileExistByUri(context, UriUtils.getLocalUriFromString(localThumb))) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    try {
                                        return ImageUtils.decodeScaleImage(context, UriUtils.getLocalUriFromString(localThumb), maxWidth, maxHeight);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        return null;
                    }

                }

                @Override
                protected void onPostExecute(Bitmap result) {
                    super.onPostExecute(result);
                    if (result != null) {
                        ViewGroup.LayoutParams params = EaseImageUtils.showImage(iv, result, maxWidth, maxHeight);
                        setBubbleView(params.width, params.height);
                        EaseImageCache.getInstance().put(localThumb, result);
                    } else {
                        if (message.status() == EMMessage.Status.FAIL) {
                            if (EaseCommonUtils.isNetWorkConnected(context)) {
                                EMClient.getInstance().chatManager().downloadThumbnail(message);
                            }
                        }

                    }
                }
            }.execute();
        }
        
    }

    private void getScreenInfo(Context context) {
        int[] imageMaxSize = EaseImageUtils.getImageMaxSize(context);
        maxWidth = imageMaxSize[0];
        maxHeight = imageMaxSize[1];
    }

    private void setBubbleView(int width, int height) {
        ViewGroup.LayoutParams params = bubbleLayout.getLayoutParams();
        params.width = width;
        params.height = height;
    }

}
