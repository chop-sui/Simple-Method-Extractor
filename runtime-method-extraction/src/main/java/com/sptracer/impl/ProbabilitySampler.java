package com.sptracer.impl;

import com.sptracer.Id;

/**
 * This implementation of {@link Sampler} samples based on a sampling probability (or sampling rate) between 0.0 and 1.0.
 * <p>
 * A sampling rate of 0.5 means that 50% of all transactions should be {@linkplain Sampler sampled}.
 * </p>
 * <p>
 * Implementation notes:
 * </p>
 * We are taking advantage of the fact, that the {@link Id} is randomly generated.
 * So instead of generating another random number,
 * we just see if the long value returned by {@link Id#getLeastSignificantBits()}
 * falls into the range between the {@code lowerBound} and the <code>higherBound</code>.
 * This is a visual representation of the mechanism with a sampling rate of 0.5 (=50%):
 * <pre>
 * Long.MIN_VALUE        0                     Long.MAX_VALUE
 * v                     v                     v
 * [----------[----------|----------]----------]
 *            ^                     ^
 *            lowerBound            higherBound = Long.MAX_VALUE * samplingRate
 *            = Long.MAX_VALUE * samplingRate * -1
 * </pre>
 */
public class ProbabilitySampler implements Sampler {

    private final long lowerBound;
    private final long higherBound;
    private final double sampleRate;

    // Because header value only contains sampling rate, we can cache it here
    private final String traceStateHeader;

    private ProbabilitySampler(double samplingRate) {
        this.higherBound = (long) (Long.MAX_VALUE * samplingRate);
        this.lowerBound = -higherBound;
        this.sampleRate = samplingRate;
        this.traceStateHeader = TraceState.getHeaderValue(samplingRate);
    }

    public static Sampler of(double samplingRate) {
        if (samplingRate == 1) {
            return ConstantSampler.of(true);
        }
        if (samplingRate == 0) {
            return ConstantSampler.of(false);
        }
        return new ProbabilitySampler(samplingRate);
    }

    @Override
    public boolean isSampled(Id traceId) {
        final long leastSignificantBits = traceId.getLeastSignificantBits();
        return leastSignificantBits > lowerBound && leastSignificantBits < higherBound;
    }

    @Override
    public double getSampleRate() {
        return sampleRate;
    }

    @Override
    public String getTraceStateHeader() {
        return traceStateHeader;
    }
}
