/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.TileCache;
import javax.swing.*;

import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.BadColorProfileException;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;

import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>LCTIFFReader</code> is a Java wrapper around the LibTIFF library.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.remotesensing.org/libtiff/">LibTIFF</a>
 */
public final class LCTIFFReader extends LCTIFFCommon {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>LCTIFFReader</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     */
    public LCTIFFReader( String fileName ) throws LCImageLibException {
        this( fileName, false );
    }

    /**
     * Construct an <code>LCTIFFReader</code> and open a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     * @param read2nd If <code>true</code>, read the second TIFF image (if
     * present).
     */
    public LCTIFFReader( String fileName, boolean read2nd )
        throws LCImageLibException
    {
        m_read2nd = read2nd;
        openForReading( fileName );
        //
        // If openForReading() fails, it will store 0 in the native pointer.
        //
        if ( m_nativePtr == 0 )
            throw new LCImageLibException( "Could not open " + fileName );
    }

    /**
     * Get the ICC profile of the TIFF image.
     *
     * @return Returns the {@link ICC_Profile} or <code>null</code> if the
     * image doesn't have a color profile.
     */
    public ICC_Profile getICCProfile() throws ColorProfileException {
        final byte[] iccProfileData = getICCProfileData();
        if ( iccProfileData == null )
            return null;
        try {
            return ICC_Profile.getInstance( iccProfileData );
        }
        catch ( IllegalArgumentException e ) {
            throw new BadColorProfileException( null );
        }
    }

    /**
     * Gets the TIFF image.
     *
     * @param thread The thread doing the getting.
     * @return Returns said image.
     */
    public synchronized PlanarImage getImage( ProgressThread thread )
        throws LCImageLibException, UserCanceledException
    {
        if ( m_image == null ) {
            try {
                if ( m_read2nd && isLightZoneLayeredTIFF() &&
                     !nextDirectory() )
                    return null;
                readImage( thread );
            }
            finally {
                dispose();
            }
        }
        return m_image;
    }

    /**
     * Gets the value of the given TIFF integer metadata field.
     *
     * @param tagID The tag ID of the metadata field to get.  The ID should be
     * that of a tag whose value is an integer ({@link #TIFF_FIELD_TYPE_USHORT}
     * or {@link #TIFF_FIELD_TYPE_ULONG} and not a string.
     * @return Returns said value.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an
     * integer metadata field or is otherwise unsupported.
     * @see #getStringField(int)
     */
    public native int getIntField( int tagID );

    /**
     * Gets one of the values of the given TIFF two-value integer metadata
     * field.
     *
     * @param tagID The tag ID of the metadata field to get.  The ID should be
     * that of a tag whose value is an integer ({@link #TIFF_FIELD_TYPE_USHORT}
     * or {@link #TIFF_FIELD_TYPE_ULONG} and not a string.
     * @param getSecond If <code>true</code>, gets the second value; otherwise
     * gets the first.
     * @return Returns said value.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an
     * integer metadata field or is otherwise unsupported.
     * @see #getStringField(int)
     */
    public native int getIntField2( int tagID, boolean getSecond );

    /**
     * Gets the number of strips in the TIFF image.  The image must be stored
     * in strips and not tiles.
     *
     * @return Returns said number of strips.
     * @see #isTiled()
     */
    public native int getNumberOfStrips();

    /**
     * Gets the number of tiles in the TIFF image.  The image must be stored
     * in tiles and not strips.
     *
     * @return Returns said number of tiles.
     * @see #isTiled()
     */
    public native int getNumberOfTiles();

    /**
     * Gets the value of the given TIFF string metadata field.
     *
     * @param tagID The tag ID of the metadata field to set.  The ID should be
     * that of a tag whose value is a string ({@link #TIFF_FIELD_TYPE_ASCII}.
     * @return Returns the string value.
     * @throws IllegalArgumentException if <code>tagID</code> isn't that of an
     * string metadata field or is otherwise unsupported.
     * @see #getIntField(int)
     * @see #getIntField2(int,boolean)
     */
    public native String getStringField( int tagID );

    /**
     * Returns the size of the strips in bytes.  The image must be stored in
     * strips and not in tiles.
     *
     * @return Returns said size.
     * @see #isTiled()
     */
    public native int getStripSize();

    /**
     * Returns the size of the tiles in bytes.  The image must be stored in
     * tiles and not in strips.
     *
     * @return Returns said size.
     * @see #isTiled()
     */
    public native int getTileSize();

    /**
     * Returns whether the TIFF image is stored in tiles.
     *
     * @return Returns <code>true</code> only if the image is stored in tiles.
     */
    public native boolean isTiled();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the raw ICC profile data (minus the header) from the TIFF image.
     *
     * @return Returns said data as a <code>byte</code> array.
     */
    private native byte[] getICCProfileData();

    /**
     * Checks whether the current TIFF file is a 2-page (layered) TIFF file
     * created by LightZone.
     *
     * @return Returns <code>true</code> only if the TIFF file is a LightZone
     * layers TIFF file.
     */
    private boolean isLightZoneLayeredTIFF() {
        final String software = getStringField( TIFF_SOFTWARE );
        if ( software == null || !software.startsWith( "LightZone" ) )
            return false;
        //
        // The TIFF has to have exactly 2 pages.
        //
        final int pages = getIntField2( TIFF_PAGE_NUMBER, true );
        return pages == 2;
    }

    /**
     * Reads the next TIFF directory replacing the current one.
     *
     * @return Returns <code>true</code> if the next directory was read
     * successfully.
     */
    private native boolean nextDirectory() throws LCImageLibException;

    /**
     * Opens a TIFF file.
     *
     * @param fileName The name of the TIFF file to open.
     */
    private native void openForReading( String fileName )
        throws LCImageLibException;

    private class TIFF_Format {
        final int imageWidth      = getIntField(TIFF_IMAGE_WIDTH);
        final int imageHeight     = getIntField(TIFF_IMAGE_LENGTH);
        final int bitsPerSample   = getIntField(TIFF_BITS_PER_SAMPLE);
        final int samplesPerPixel = getIntField(TIFF_SAMPLES_PER_PIXEL);
        final int sampleFormat    = getIntField(TIFF_SAMPLE_FORMAT);
        final int extraSamples    = getIntField(TIFF_EXTRA_SAMPLES);
        final int photometric     = getIntField(TIFF_PHOTOMETRIC_INTERPRETATION);
        final int planarConfig    = getIntField(TIFF_PLANAR_CONFIGURATION);
        final int planes          = planarConfig == TIFF_PLANAR_CONFIGURATION_CHUNKY ? 1 : samplesPerPixel;
        final boolean tiled       = isTiled();

        final int tiles           = tiled ? getNumberOfTiles() / planes : getNumberOfStrips() / planes;
        final int tileSize        = tiled ? getTileSize() : getStripSize();
        final int tiffTileWidth   = tiled ? getIntField(TIFF_TILE_WIDTH) : imageWidth;
        final int tiffTileHeight  = tiled ? getIntField(TIFF_TILE_LENGTH)
                                          : tileSize / (imageWidth * (samplesPerPixel / planes) * bitsPerSample / 8);

        final boolean hasAlpha = extraSamples == TIFF_EXTRA_SAMPLES_ASSOC_ALPHA || extraSamples == TIFF_EXTRA_SAMPLES_UNASSOS_ALPHA;
        final boolean isAlphaPremultiplied = extraSamples == TIFF_EXTRA_SAMPLES_ASSOC_ALPHA;

        final ICC_Profile profile;
        final ColorSpace colorSpace;
        final ComponentColorModel tiffCcm;
        final SampleModel tiffTsm;

        TIFF_Format() throws LCImageLibException {
            if (bitsPerSample != 8 && bitsPerSample != 16)
                throw new LCImageLibException("Unsupported TIFF Bit per Sample Value: " + bitsPerSample);

            ICC_Profile iccProfile = null;
            try {
                iccProfile = getICCProfile();
                // TODO: deal with weird photometric interpretations
                if ( iccProfile == null && photometric == TIFF_PHOTOMETRIC_CIELAB )
                    iccProfile = JAIContext.labProfile;
            }
            catch ( ColorProfileException e ) {
                // nothing
            }
            profile = iccProfile;

            if (profile != null)
                colorSpace = new ICC_ColorSpace(profile);
            else
                switch ( samplesPerPixel - (hasAlpha ? 1 : 0) ) {
                    case 1:
                        colorSpace = JAIContext.gray22ColorSpace;
                        break;
                    case 3:
                        colorSpace = JAIContext.sRGBColorSpace;
                        break;
                    case 4:
                        colorSpace = JAIContext.CMYKColorSpace;
                        break;
                    default:
                        throw new LCImageLibException(
                            "Bad image: " + samplesPerPixel + " samples per pixel."
                        );
                }

            // Color model for the tiff image, can have an alpha channel

            tiffCcm = new ComponentColorModel(
                colorSpace,
                hasAlpha,
                isAlphaPremultiplied,
                Transparency.OPAQUE,
                bitsPerSample == 8
                    ? DataBuffer.TYPE_BYTE
                    : sampleFormat == TIFF_SAMPLE_FORMAT_INT ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT
            );

            // Sample model for the readout buffer, large enough to hold a tile or a strip of the TIFF image, can be banded
            if ( planarConfig == TIFF_PLANAR_CONFIGURATION_CHUNKY )
                tiffTsm = tiffCcm.createCompatibleSampleModel(tiffTileWidth, tiffTileHeight);
            else
                tiffTsm = new BandedSampleModel(tiffCcm.getTransferType(), tiffTileWidth, tiffTileHeight, samplesPerPixel);

        }
    }

    private TIFF_Format getFormat() throws LCImageLibException {
        return new LCTIFFReader.TIFF_Format();
    }

    public static class TIFFImage extends PlanarImage {
        final LCTIFFReader.TIFF_Format tf;
        final LCTIFFReader reader;

        public TIFFImage(String path) throws LCImageLibException {
            reader = new LCTIFFReader(path);
            tf = reader.getFormat();
            final ImageLayout layout = new ImageLayout(0, 0, tf.imageWidth, tf.imageHeight,
                                                       0, 0,
                                                       tf.tiffTileWidth, tf.tiffTileHeight,
                                                       tf.tiffTsm, tf.tiffCcm);
            this.setImageLayout(layout);
        }

        public void dispose() {
            super.dispose();
            reader.dispose();
        }

        protected transient TileCache cache = JAIContext.defaultTileCache;
        protected Object tileCacheMetric;

        public TileCache getTileCache() {
            return cache;
        }

        public void setTileCache(TileCache cache) {
            if (this.cache != null) {
                this.cache.removeTiles(this);
            }
            this.cache = cache;
        }

        protected Raster getTileFromCache(int tileX, int tileY) {
            return cache != null ? cache.getTile(this, tileX, tileY) : null;
        }

        protected void addTileToCache(int tileX,
                                      int tileY,
                                      Raster tile) {
            if (cache != null) {
                cache.add(this, tileX, tileY, tile, null);
            }
        }

        public Raster getTile(int tileX, int tileY) {
            Raster tile = null;	// the requested tile, to be returned

            int tileN = tileX + tileY * this.getNumXTiles();

            // Make sure the requested tile is inside this image's boundary.
            if (tileX >= getMinTileX() && tileX <= getMaxTileX() &&
                tileY >= getMinTileY() && tileY <= getMaxTileY()) {
                // Check if tile is available in the cache.
                tile = getTileFromCache(tileX, tileY);

                if (tile == null) {         // tile not in cache
                    tile = Raster.createWritableRaster(tf.tiffTsm, new Point(tileX * tf.tiffTileWidth, tileY * tf.tiffTileHeight));

                    try {
                        for ( int plane = 0; plane < tf.planes; plane++ ) {
                            if ( tf.tiled ) {
                                if ( tf.bitsPerSample == 8 ) {
                                    final byte[] buffer = ((DataBufferByte) tile.getDataBuffer()).getData( plane );
                                    reader.readTileByte( tileN + plane * tf.tiles, buffer, 0, tf.tileSize );
                                } else {
                                    final short[] buffer = ((DataBufferUShort) tile.getDataBuffer()).getData( plane );
                                    reader.readTileShort( tileN + plane * tf.tiles, buffer, 0, tf.tileSize );
                                }
                            } else {
                                if ( tf.bitsPerSample == 8 ) {
                                    final byte[] buffer = ((DataBufferByte) tile.getDataBuffer()).getData( plane );
                                    reader.readStripByte( tileY + plane * tf.tiles, buffer, 0, tf.tileSize );
                                } else {
                                    final short[] buffer = ((DataBufferUShort) tile.getDataBuffer()).getData( plane );
                                    reader.readStripShort( tileY + plane * tf.tiles, buffer, 0, tf.tileSize );
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Cache the result tile.
                    addTileToCache(tileX, tileY, tile);
                }
            }

            return tile;
        }
    }

    /**
     * Reads the TIFF image.
     *
     * @param thread The thread that is doing the reading.
     */
    private void readImage( ProgressThread thread )
        throws LCImageLibException, UserCanceledException
    {
        final TIFF_Format tf = new TIFF_Format();

        // Color model for a LightZone Image, no alpha chaannel
        final ComponentColorModel imageCcm = new ComponentColorModel(
            tf.colorSpace,
            false,
            false,
            Transparency.OPAQUE,
            tf.bitsPerSample == 8
                ? DataBuffer.TYPE_BYTE
                : tf.sampleFormat == TIFF_SAMPLE_FORMAT_INT ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_USHORT
        );

        // The readout buffer itself
        DataBuffer db = null;
        if (!(tf.tiled && !tf.hasAlpha)) {
            /* If the TIFF image is tiled and it doesn't have an alpha channel
               then we can read directly into the destination image buffer,
               don't allocate the buffer */
            if (tf.bitsPerSample == 8)
                db = tf.planes == 1 ? new DataBufferByte(tf.tileSize) : new DataBufferByte(tf.tileSize, tf.planes);
            else
                db = tf.planes == 1 ? new DataBufferUShort(tf.tileSize/2) : new DataBufferUShort(tf.tileSize/2, tf.planes);
        }
        // Sample model for the output image, interleaved
        final SampleModel tsm = imageCcm.createCompatibleSampleModel(JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT);

        // Layout of the output image
        final ImageLayout layout = new ImageLayout(0, 0, tf.imageWidth, tf.imageHeight,
                                                   0, 0,
                                                   JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                                                   tsm, imageCcm);

        // The output image itself, directly allocated in the file cache
        CachedImage image = new CachedImage(layout, JAIContext.fileCache);

        final int maxTileX = image.getNumXTiles();

        ProgressIndicator indicator = null;
        if ( thread != null )
            indicator = thread.getProgressIndicator();
        if ( indicator != null )
            indicator.setMaximum( tf.tiles );

        int bandList[] = new int[tf.samplesPerPixel];
        for (int i = 0; i < tf.samplesPerPixel; i++)
            bandList[i] = i;

        Rectangle imageBounds = new Rectangle(0, 0, tf.imageWidth, tf.imageHeight);

        for ( int tile = 0; tile < tf.tiles; tile++ ) {
            if ( thread != null && thread.isCanceled() )
                throw new UserCanceledException();
            final int tileX = tf.tiled ? tile % maxTileX : 0;
            final int tileY = tf.tiled ? tile / maxTileX : tile;

            // The actual tile bounds, clipping on the image bounds
            Rectangle tileBounds = new Rectangle(tileX * tf.tiffTileWidth,
                                                 tileY * tf.tiffTileHeight,
                                                 tf.tiffTileWidth,
                                                 tf.tiffTileHeight).intersection(imageBounds);

            // the corresponding tile data
            int tileData = (tf.samplesPerPixel / tf.planes) * tileBounds.width * tileBounds.height * (tf.bitsPerSample == 8 ? 1 : 2);

            /* If the TIFF image is tiled and it doesn't have an alpha channel
               then we can read directly into the destination image buffer,
               don't allocate an intermediate raster */

            WritableRaster raster =
                    !tf.tiled || tf.hasAlpha
                    ? Raster.createWritableRaster(tf.tiffTsm, db, new Point(tileBounds.x, tileBounds.y))
                    : null;

            for ( int plane = 0; plane < tf.planes; plane++ ) {
                if ( tf.tiled ) {
                    if (!tf.hasAlpha)
                        raster = image.getWritableTile( tileX, tileY );

                    if ( tf.bitsPerSample == 8 ) {
                        final byte[] buffer =
                            ((DataBufferByte)raster.getDataBuffer()).getData(
                                plane );
                        int read = readTileByte( tile + plane * tf.tiles, buffer, 0, tileData );
                        if (read != tileData)
                            throw new LCImageLibException("Broken TIFF File");
                    } else {
                        final short[] buffer =
                            ((DataBufferUShort)raster.getDataBuffer()).getData(
                                plane );
                        int read = readTileShort( tile + plane * tf.tiles, buffer, 0, tileData );
                        if (read != tileData)
                            throw new LCImageLibException("Broken TIFF File");
                    }
                } else {
                    if ( tf.bitsPerSample == 8 ) {
                        final byte[] buffer =
                            ((DataBufferByte)db).getData( plane );
                        int read = readStripByte( tileY + plane * tf.tiles, buffer, 0, tileData );
                        if (read != tileData)
                            throw new LCImageLibException("Broken TIFF File");
                    } else {
                        final short[] buffer =
                            ((DataBufferUShort)db).getData( plane );
                        int read = readStripShort( tileY + plane * tf.tiles, buffer, 0, tileData );
                        if (read != tileData)
                            throw new LCImageLibException("Broken TIFF File");
                    }
                }
            }
            if (!tf.tiled || tf.hasAlpha)
                image.setData(raster.createChild(raster.getMinX(), raster.getMinY(),
                                                 raster.getWidth(), raster.getHeight(),
                                                 raster.getMinX(), raster.getMinY(),
                                                 bandList));
            if ( indicator != null )
                indicator.incrementBy( 1 );
        }
        if ( indicator != null )
            indicator.setIndeterminate( true );

        m_image = image;
    }

    /**
     * NOTE: TIFF read functions can be called in parallel for different tiles, make them synchronized...
     */

    /**
     * Reads and decodes and encoded strip from the TIFF image.  The image must
     * be stored in strips and not tiles.
     *
     * @param stripIndex The index of the strip to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin
     * being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see #isTiled()
     * @see #getStripSize()
     */
    private native synchronized int readStripByte( int stripIndex, byte[] buf, long offset,
                                                   int stripSize )
        throws LCImageLibException;

    /**
     * Reads and decodes and encoded strip from the TIFF image.  The image must
     * be stored in strips and not tiles.
     *
     * @param stripIndex The index of the strip to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin
     * being placed.
     * @param stripSize The size of the strip.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see #isTiled()
     * @see #getStripSize()
     */
    private native synchronized int readStripShort( int stripIndex, short[] buf, long offset,
                                                    int stripSize )
        throws LCImageLibException;

    /**
     * Reads and decodes and encoded tile from the TIFF image.  The image must
     * be stored in tiles and not strips.
     *
     * @param tileIndex The index of the tile to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin
     * being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see #isTiled()
     * @see #getTileSize()
     */
    private native synchronized int readTileByte( int tileIndex, byte[] buf, long offset,
                                                  int tileSize ) throws LCImageLibException;

    /**
     * Reads and decodes and encoded tile from the TIFF image.  The image must
     * be stored in tiles and not strips.
     *
     * @param tileIndex The index of the tile to read.
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin
     * being placed.
     * @param tileSize The size of the tile.
     * @return Returns the number of bytes read or -1 if there was an error.
     * @see #isTiled()
     * @see #getTileSize()
     */
    private native synchronized int readTileShort( int tileIndex, short[] buf, long offset,
                                                   int tileSize ) throws LCImageLibException;

    /**
     * The actual end-result image.
     */
    private PlanarImage m_image;

    /**
     * If <code>true</code>, read the second TIFF image (if present).
     */
    private final boolean m_read2nd;

    ////////// main() /////////////////////////////////////////////////////////

    public static void main(String[] args) {
        try {
            // final LCTIFFReader tiff = new LCTIFFReader(args[0]);
            // final PlanarImage image = tiff.getImage( null );

            final PlanarImage image = new TIFFImage(args[0]);

            final JFrame frame = new JFrame("TIFF Image");
            final JPanel imagePanel = new JPanel() {
                public void paintComponent(Graphics g) {
                    ((Graphics2D) g).drawRenderedImage(image, new AffineTransform());
                }
            };
            imagePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
            final JScrollPane scrollPane = new JScrollPane(imagePanel);
            frame.setContentPane(scrollPane);
            frame.pack();
            frame.setSize(800, 600);
            frame.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
/* vim:set et sw=4 ts=4: */
