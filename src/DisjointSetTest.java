/**
 * Checks of Mission 0. Run:  java -cp out DisjointSetTest
 * Add your own checks below: a large chain, a union of a set with itself, count after many unions.
 */
public class DisjointSetTest {

    static int failed = 0;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok    " : "FAIL  ") + what);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        DisjointSet d = new DisjointSet(6);
        check(d.count() == 6, "6 sets at the start");
        check(!d.connected(0, 1), "0 and 1 are apart at the start");
        check(d.union(0, 1), "union(0, 1) joins two sets");
        check(d.connected(0, 1), "0 and 1 are together");
        check(!d.union(1, 0), "union(1, 0) again returns false");
        check(d.count() == 5, "5 sets after one union");
        d.union(2, 3); d.union(1, 3);
        check(d.connected(0, 2), "0 and 2 are together through 1 and 3");
        check(d.count() == 3, "3 sets: {0,1,2,3} {4} {5}");
        check(d.find(4) == 4 && d.find(5) == 5, "a single cell is its own root");

        check(!d.union(4, 4), "union with itself changes nothing");
        check(d.count() == 3, "self union does not change count");
        DisjointSet chain = new DisjointSet(100_000);
        boolean joined = true;
        for (int i = 1; i < 100_000; i++) joined &= chain.union(i - 1, i);
        check(joined, "all consecutive unions join different sets");
        check(chain.count() == 1, "one set after 99999 unions");
        check(chain.connected(0, 99_999), "ends of a large chain are connected");
        check(!chain.union(0, 99_999), "repeated union returns false");
        check(chain.find(50_000) == chain.find(0), "middle has the same root");
        check(new DisjointSet(0).count() == 0, "zero cells means zero sets");
        DisjointSet single = new DisjointSet(1);
        check(single.find(0) == 0 && single.count() == 1, "single cell works");

        System.out.println(failed == 0 ? "All checks passed." : failed + " checks failed.");
    }
}
