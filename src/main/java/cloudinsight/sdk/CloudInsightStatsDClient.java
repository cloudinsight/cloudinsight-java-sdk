package cloudinsight.sdk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class CloudInsightStatsDClient implements StatsDClient {
    
    private static final int PACKET_SIZE_BYTES = Integer.valueOf(1500);
    
    private static final StatsDClientErrorHandler NO_OP_HANDLER = new StatsDClientErrorHandler() {
       public void handle(Exception e) { 
            
        }
    };
    
    private static final ThreadLocal<NumberFormat> NUMBER_FORMATTERS = new ThreadLocal<NumberFormat>() {
        @Override
        protected NumberFormat initialValue() {

            NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
            numberFormatter.setGroupingUsed(false);
            numberFormatter.setMaximumFractionDigits(6);

            if (numberFormatter instanceof DecimalFormat) { 
                final DecimalFormat decimalFormat = (DecimalFormat) numberFormatter;
                final DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
                symbols.setNaN("NaN");
                decimalFormat.setDecimalFormatSymbols(symbols);
            }

            return numberFormatter;
        }
    };
    
    private final String                           prefix;
    private final DatagramChannel                  clientChannel;
    private final InetSocketAddress                address;
    private final StatsDClientErrorHandler         handler;
    private final String                           constantTagsRendered;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        final ThreadFactory delegate = Executors.defaultThreadFactory();
        public Thread newThread(Runnable r) {
            Thread result = delegate.newThread(r);
            result.setName("Ci-StatsD-" + result.getName());
            result.setDaemon(true);
            return result;
        }
    });
    
    private final BlockingQueue<String>            queue             = new LinkedBlockingQueue<String>();

    private static final String                    DEFAULT_PREFIX    = "";

    private static final String                    DEFAULT_HOSTNAME  = "localhost";

    private static final int                       DEFAULT_PORT      = Integer.valueOf(8251);

    public CloudInsightStatsDClient() {
        this(DEFAULT_PREFIX, DEFAULT_HOSTNAME, DEFAULT_PORT);
    }

    public CloudInsightStatsDClient(String prefix, String hostname, int port) throws StatsDClientException {
        this(prefix, hostname, port, null, NO_OP_HANDLER);
    }

    public CloudInsightStatsDClient(String prefix, String hostname, int port, String... constantTags) throws StatsDClientException {
        this(prefix, hostname, port, constantTags, NO_OP_HANDLER);
    }
    
    public CloudInsightStatsDClient(String prefix, String hostname, int port, String[] constantTags, StatsDClientErrorHandler errorHandler) throws StatsDClientException {
        if (prefix != null && prefix.length() > 0) {
            this.prefix = String.format("%s.", prefix);
        } else {
            this.prefix = "";
        }
        this.handler = errorHandler;

        if (constantTags != null && constantTags.length == 0) {
            constantTags = null;
        }

        if (constantTags != null) {
            this.constantTagsRendered = tagString(constantTags, null);
        } else {
            this.constantTagsRendered = null;
        }

        try {
            this.clientChannel = DatagramChannel.open();
            this.address = new InetSocketAddress(hostname, port);
        } catch (Exception e) {
            throw new StatsDClientException("Failed to start StatsD client", e);
        }
        this.executor.submit(new QueueConsumer());
    }

    static String tagString(final String[] tags, final String tagPrefix) {
        StringBuilder sb;
        if (tagPrefix != null) {
            if (tags == null || tags.length == 0) {
                return tagPrefix;
            }
            sb = new StringBuilder(tagPrefix);
            sb.append(",");
        } else {
            if (tags == null || tags.length == 0) {
                return "";
            }
            sb = new StringBuilder("|#");
        }

        for (int n = tags.length - 1; n >= 0; n--) {
            sb.append(tags[n]);
            if (n > 0) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    String tagString(final String[] tags) {
        return tagString(tags, constantTagsRendered);
    }

    /**
     * send message to agent.
     * 
     * @param message
     */
    private void send(String message) {
        queue.offer(message);
    }

    public void stop() {
        try {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            handler.handle(e);
        }
        finally {
            if (clientChannel != null) {
                try {
                    clientChannel.close();
                }
                catch (IOException e) {
                    handler.handle(e);
                }
            }
        }
    }

    public void count(String aspect, long delta, String... tags) {
        send(String.format("%s%s:%d|c%s", prefix, aspect, delta, tagString(tags)));
    }

    public void incrementCounter(String aspect, String... tags) {
        count(aspect, 1, tags);
    }

    public void increment(String aspect, String... tags) {
        incrementCounter(aspect, tags);
    }

    public void decrementCounter(String aspect, String... tags) {
        count(aspect, -1, tags);
    }
   
    public void decrement(String aspect, String... tags) {
        decrementCounter(aspect, tags);
    }
  
    public void recordGaugeValue(String aspect, double value, String... tags) {
        send(String.format("%s%s:%s|g%s", prefix, aspect, NUMBER_FORMATTERS.get().format(value), tagString(tags)));
    }

    public void gauge(String aspect, double value, String... tags) {
        recordGaugeValue(aspect, value, tags);
    }

    public void recordGaugeValue(String aspect, long value, String... tags) {
        send(String.format("%s%s:%d|g%s", prefix, aspect, value, tagString(tags)));
    }

    public void gauge(String aspect, long value, String... tags) {
        recordGaugeValue(aspect, value, tags);
    }

    public void recordExecutionTime(String aspect, long timeInMs, String... tags) {
        send(String.format("%s%s:%d|ms%s", prefix, aspect, timeInMs, tagString(tags)));
    }

    public void time(String aspect, long value, String... tags) {
        recordExecutionTime(aspect, value, tags);
    }

    public void recordHistogramValue(String aspect, double value, String... tags) {
        send(String.format("%s%s:%s|h%s", prefix, aspect, NUMBER_FORMATTERS.get().format(value), tagString(tags)));
    }

    public void histogram(String aspect, double value, String... tags) {
        recordHistogramValue(aspect, value, tags);
    }

    public void recordHistogramValue(String aspect, long value, String... tags) {
        send(String.format("%s%s:%d|h%s", prefix, aspect, value, tagString(tags)));
    }

    public void histogram(String aspect, long value, String... tags) {
        recordHistogramValue(aspect, value, tags);
    }
    
    public static final Charset MESSAGE_CHARSET = Charset.forName("UTF-8");

    private class QueueConsumer implements Runnable {
        private final ByteBuffer sendBuffer = ByteBuffer.allocate(PACKET_SIZE_BYTES);

         public void run() {
            while(!executor.isShutdown()) {
                try {
                    String message = queue.poll(1, TimeUnit.SECONDS);
                    if(null != message) {
                        byte[] data = message.getBytes(MESSAGE_CHARSET);
                        if(sendBuffer.remaining() < (data.length + 1)) {
                            blockingSend();
                        }
                        if(sendBuffer.position() > 0) {
                            sendBuffer.put( (byte) '\n');
                        }
                        sendBuffer.put(data);
                        if(null == queue.peek()) {
                            blockingSend();
                        }
                    }
                } catch (Exception e) {
                    handler.handle(e);
                }
            }
        }

        private void blockingSend() throws IOException {
            int sizeOfBuffer = sendBuffer.position();
            sendBuffer.flip();
            int sentBytes = clientChannel.send(sendBuffer, address);
            sendBuffer.limit(sendBuffer.capacity());
            sendBuffer.rewind();

            if (sizeOfBuffer != sentBytes) {
                handler.handle(
                        new IOException(
                            String.format(
                                "Could not send entirely stat %s to host %s:%d. Only sent %d bytes out of %d bytes",
                                sendBuffer.toString(),
                                address.getHostName(),
                                address.getPort(),
                                sentBytes,
                                sizeOfBuffer)));
            }
        }
    }
}