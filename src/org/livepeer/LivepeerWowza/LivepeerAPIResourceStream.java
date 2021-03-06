package org.livepeer.LivepeerWowza;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wowza.wms.media.model.MediaCodecInfoVideo;
import com.wowza.wms.rest.ShortObject;
import com.wowza.wms.rest.vhosts.applications.transcoder.TranscoderAppConfig;
import com.wowza.wms.rest.vhosts.applications.transcoder.TranscoderTemplateAppConfig;

import java.util.*;

/**
 * Representation of LivepeerAPI's "/stream" object
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class LivepeerAPIResourceStream {
  public final static String LIVEPEER_PREFIX = "livepeer_";

  Wowza wowza = new Wowza();
  // Don't push up a null id on the initial create
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String id;
  private String name;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> presets = new ArrayList<String>();
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> errors;
  private Map<String, String> renditions = new HashMap<String, String>();
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<Profile> profiles;

  /**
   * Create an empty API Resource. Mostly used by the JSON serializer when GETing objects
   */
  public LivepeerAPIResourceStream() {

  }

  /**
   * Create a Wowza resource from internal configuration, suitable for POSTing
   *
   * @param vhost
   * @param application
   */
  public LivepeerAPIResourceStream(String vhost, String application, MediaCodecInfoVideo codecInfoVideo) {
    id = null;

    TranscoderAppConfig transcoderAppConfig = new TranscoderAppConfig(vhost, application);
    transcoderAppConfig.loadObject();
    wowza.setTranscoderAppConfig(transcoderAppConfig);

    for (ShortObject t : transcoderAppConfig.getTemplates().getTemplates()) {
      TranscoderTemplateAppConfig ttac = new TranscoderTemplateAppConfig(vhost, application, t.getId());
      ttac.loadObject();
      wowza.getTranscoderTemplateAppConfig().put(t.getId(), ttac);
    }

    LivepeerAPIResourceStreamWowzaSourceInfo sourceInfo = new LivepeerAPIResourceStreamWowzaSourceInfo();
    sourceInfo.setWidth(codecInfoVideo.getVideoWidth());
    sourceInfo.setHeight(codecInfoVideo.getVideoHeight());
    sourceInfo.setFps(codecInfoVideo.getFrameRate());
    this.wowza.setSourceInfo(sourceInfo);
  }

  /**
   * Get the id of this stream
   * @return id of the stream
   */
  public String getId() {
    return id;
  }

  /**
   * Set the id of the stream
   * @param _id
   */
  public void setId(String _id) {
    id = _id;
  }

  public List<String> getPresets() {
    return presets;
  }

  public void setPresets(List<String> presets) {
    this.presets = presets;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public Map<String, String> getRenditions() {
    return renditions;
  }

  public void setRenditions(Map<String, String> renditions) {
    this.renditions = renditions;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Wowza getWowza() {
    return wowza;
  }

  public void setWowza(Wowza wowza) {
    this.wowza = wowza;
  }

  public List<Profile> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<Profile> profiles) {
    this.profiles = profiles;
  }

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Profile {
    private String name;
    private double fps;
    private int bitrate;
    private int width;
    private int height;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public double getFps() {
      return fps;
    }

    public void setFps(double fps) {
      this.fps = fps;
    }

    public int getBitrate() {
      return bitrate;
    }

    public void setBitrate(int bitrate) {
      this.bitrate = bitrate;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }
  }


  /**
   * Class representing the "wowza" subfield.
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class Wowza {
    private Map<String, TranscoderTemplateAppConfig> transcoderTemplateAppConfig = new HashMap<>();
    private TranscoderAppConfig transcoderAppConfig;
    private LivepeerAPIResourceStreamWowzaSourceInfo sourceInfo = new LivepeerAPIResourceStreamWowzaSourceInfo();

    private List<LivepeerAPIResourceStreamWowzaStreamNameGroup> streamNameGroups = new ArrayList<>();

    public TranscoderAppConfig getTranscoderAppConfig() {
      return transcoderAppConfig;
    }

    public void setTranscoderAppConfig(TranscoderAppConfig tac) {
      transcoderAppConfig = tac;
    }

    public Map<String, TranscoderTemplateAppConfig> getTranscoderTemplateAppConfig() {
      return transcoderTemplateAppConfig;
    }

    public void setTranscoderTemplateAppConfig(Map<String, TranscoderTemplateAppConfig> ttac) {
      transcoderTemplateAppConfig = ttac;
    }

    public List<LivepeerAPIResourceStreamWowzaStreamNameGroup> getStreamNameGroups() {
      return streamNameGroups;
    }

    public void setStreamNameGroups(List<LivepeerAPIResourceStreamWowzaStreamNameGroup> streamNameGroups) {
      this.streamNameGroups = streamNameGroups;
    }

    public LivepeerAPIResourceStreamWowzaSourceInfo getSourceInfo() {
      return sourceInfo;
    }

    public void setSourceInfo(LivepeerAPIResourceStreamWowzaSourceInfo sourceInfo) {
      this.sourceInfo = sourceInfo;
    }
  }

  /**
   * Class representing entries in the "streamNameGroups" subfield
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class LivepeerAPIResourceStreamWowzaStreamNameGroup {
    private String name;
    private List<String> renditions;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getRenditions() {
      return renditions;
    }

    public void setRenditions(List<String> renditions) {
      this.renditions = renditions;
    }
  }

  /**
   * Class representing entries in the "streamNameGroups" subfield
   */
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public static class LivepeerAPIResourceStreamWowzaSourceInfo {
    private int width;
    private int height;
    private double fps;

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }

    public double getFps() {
      return fps;
    }

    public void setFps(double fps) {
      this.fps = fps;
    }
  }
}
