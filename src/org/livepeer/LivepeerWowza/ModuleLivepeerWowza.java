package org.livepeer.LivepeerWowza;

import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livetranscoder.*;
import com.wowza.wms.transcoder.encoder.TranscoderEncoderStreamInfo;
import com.wowza.wms.module.*;
import com.wowza.wms.pushpublish.protocol.rtmp.IPushPublishRTMPNotify;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMP;
import com.wowza.wms.pushpublish.protocol.rtmp.PushPublishRTMPNetConnectionSession;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.rest.WMSClientSecurity;
import com.wowza.wms.rest.vhosts.applications.transcoder.TranscoderAppConfig;

import java.io.OutputStream;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.restlet.ext.jackson.JacksonRepresentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.*;
import com.wowza.wms.media.model.MediaCodecInfoAudio;
import com.wowza.wms.media.model.MediaCodecInfoVideo;

public class ModuleLivepeerWowza extends ModuleBase {
	class StreamListener implements IMediaStreamActionNotify3 {
		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket) {
			System.out.println("onMetaData[" + stream.getContextStr() + "]: " + metaDataPacket.toString());
		}

		public void onPauseRaw(IMediaStream stream, boolean isPause, double location) {
			System.out.println(
					"onPauseRaw[" + stream.getContextStr() + "]: isPause:" + isPause + " location:" + location);
		}

		public void onPause(IMediaStream stream, boolean isPause, double location) {
			System.out.println("onPause[" + stream.getContextStr() + "]: isPause:" + isPause + " location:" + location);
		}

		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset) {
			System.out.println("onPlay[" + stream.getContextStr() + "]: playStart:" + playStart + " playLen:" + playLen
					+ " playReset:" + playReset);
		}

		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
			System.out.println("onPublish[" + stream.getContextStr() + "]: isRecord:" + isRecord + " isAppend:"
					+ isAppend + " name:" + streamName);
			try {
				LivepeerAPI livepeer = new LivepeerAPI();
				livepeer.pushTranscodeInformation("_defaultVHost_", "live");

				PushPublishHTTPCupertinoLivepeerHandler http = new PushPublishHTTPCupertinoLivepeerHandler();
				http.setAppInstance(_appInstance);
				http.setSrcStreamName(streamName);
				http.setDstStreamName(streamName);

				http.init(_appInstance, streamName, stream, new HashMap<String, String>(),
						new HashMap<String, String>(), null, true);
//				http.load(new HashMap<String, String>());
				http.connect();
//				PushPublishRTMP publisher = new PushPublishRTMP();
//				publisher.addListener(new RTMPListener());
//
//				// Source stream
//				publisher.setAppInstance(_appInstance);
////				publisher.setSrcStream(stream);
//				publisher.setSrcStreamName(streamName);
////				publisher.setWaitOnMetadataAvailable(true);
//
//				// Destination stream
//				publisher.setHostname("35.232.200.158");
//				publisher.setPort(1935);
//				publisher.setDstApplicationName("stream");
//				publisher.setDstStreamName("eli");
//				publisher.setRemoveDefaultAppInstance(true);
//				synchronized(publishers)
//				{
//					publishers.put(stream, publisher);
//				}
//
//				publisher.connect();
//				getLogger().info("LIVEPEER connected");
			} catch (Exception e) {
				getLogger().info("LIVEPEER HTTP: ", e);
			}
		}

		public void onSeek(IMediaStream stream, double location) {
			System.out.println("onSeek[" + stream.getContextStr() + "]: location:" + location);
		}

		public void onStop(IMediaStream stream) {
			System.out.println("onStop[" + stream.getContextStr() + "]: ");
		}

		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend) {
			System.out.println("onUnPublish[" + stream.getContextStr() + "]: streamName:" + streamName + " isRecord:"
					+ isRecord + " isAppend:" + isAppend);
			synchronized (publishers) {
				PushPublishRTMP publisher = publishers.remove(stream);
				if (publisher != null)
					publisher.disconnect();
			}
		}

		public void onCodecInfoAudio(IMediaStream stream, MediaCodecInfoAudio codecInfoAudio) {
			System.out.println("onCodecInfoAudio[" + stream.getContextStr() + " Audio Codec"
					+ codecInfoAudio.toCodecsStr() + "]: ");
		}

		public void onCodecInfoVideo(IMediaStream stream, MediaCodecInfoVideo codecInfoVideo) {
			System.out.println("onCodecInfoVideo[" + stream.getContextStr() + " Video Codec"
					+ codecInfoVideo.toCodecsStr() + "]: ");
		}
	}

	private IApplicationInstance _appInstance;
	Map<IMediaStream, PushPublishRTMP> publishers = new HashMap<IMediaStream, PushPublishRTMP>();

	class TranscoderControl implements ILiveStreamTranscoderControl {
		public boolean isLiveStreamTranscode(String transcoder, IMediaStream stream) {
			// No transcoding, Livepeer is gonna take care of it
			TranscoderEncoderStreamInfo info = stream.getTranscoderEncoderStreamInfo();
			System.out.println("LIVEPEER TRANSCODER " + transcoder);
			return false;
		}
	}

	public void onAppStart(IApplicationInstance appInstance) {
		System.out.println("livepeer booting up");

//		System.out.println("LIVEPEER " + appInstance.getTranscoderProperties());
	}

	public void onStreamCreate(IMediaStream stream) {
		if (stream.getClientId() == -1) {
			getLogger().info("Ignoring local stream");
			return;
		}
		IMediaStreamActionNotify2 actionNotify = new StreamListener();

		WMSProperties props = stream.getProperties();
		synchronized (props) {
			props.put("streamActionNotifier", actionNotify);
		}

		stream.addClientListener(actionNotify);
		getLogger().info("LIVEPEER onStreamCreate[" + stream + "]: clientId:" + stream.getClientId());
		getLogger().info("LIVEPEER onStreamCreate stream=" + stream.isPublisherStream());

	}
}