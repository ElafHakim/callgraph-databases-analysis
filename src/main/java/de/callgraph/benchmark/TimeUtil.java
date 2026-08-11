package de.callgraph.benchmark;

public final class TimeUtil {

    private static final int NUMBER_OF_RUNS = 5;
    private static final long TOTAL_SIZE_MB = 812;

    private static final long[] VALUES = new long[NUMBER_OF_RUNS];

    private TimeUtil() {
        // Verhindert das Erzeugen eines Objekts
    }

    public static void setValue(int index, long totalDuration) {
        if (index < 0 || index >= VALUES.length) {
            throw new IllegalArgumentException(
                    "Ungültiger Durchlauf: " + index
            );
        }

        VALUES[index] = totalDuration;
    }

    public static long getValue(int index) {
        if (index < 0 || index >= VALUES.length) {
            throw new IllegalArgumentException(
                    "Ungültiger Durchlauf: " + index
            );
        }

        return VALUES[index];
    }

    public static long getMittelwert() {
        long sum = 0;

        for (long value : VALUES) {
            sum += value;
        }

        return sum / VALUES.length;
    }

    public static long getSecondsPer1MB() {
        return getMittelwert() / TOTAL_SIZE_MB;
    }

    public static void main(String[] args) {
        System.out.println("Erster Durchlauf: " + VALUES[0]);
        System.out.println("Mittelwert: " + getMittelwert());
        System.out.println(
                "Durchschnittliche Zeit pro MB: "
                        + getSecondsPer1MB()
        );
    }
}