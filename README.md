# FastKeyErasureRNG

**Fast-key-erasure random-number generator for Java**

A cryptographically secure [RNG](https://en.wikipedia.org/wiki/Random_number_generation), based on [AES-256](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard) “CTR” mode, with high performance and forward secrecy.

FastKeyErasureRNG has been shown to pass the [Dieharder](https://webhome.phy.duke.edu/~rgb/General/dieharder.php) random number generator testing suite.

## Algorithm

This implementation is based on *“Fast-key-erasure random-number generators”* by D. J. Bernstein.

Please see here for details:  
<https://blog.cr.yp.to/20170723-random.html>

## Usage

The **`FastKeyErasureRNG`** class implements the standard [`Random`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Random.html) interface.

Example of usage:

```java
import io.github.deajl3ka.fast_key_erasure.FastKeyErasureRNG;

public class Example {
    public static void main(String[] args) {
        final FastKeyErasureRNG random = FastKeyErasureRNG.current();
        for (int i = 0; i < 42; ++i) {
            System.out.println(random.nextLong());
        }
    }
}
```

### Thread safety

The `FastKeyErasureRNG` class is **not** thread-safe by itself.

It is recommended that each thread uses its own separate instance! Use the *static* method `FastKeyErasureRNG.current()` to obtain an instance for the current thread.

## Website

Git mirrors for this project:

* <https://github.com/dEajL3kA/FastKeyErasureRNG>
* <https://gitlab.com/deajl3ka1/fast-key-erasure-rng-java.git>
* <https://repo.or.cz/fast-key-erasure-rng-java.git>

## Contact information

E-Mail:  
<Cumpoing79@web.de>

OpenPGP key:  
[`F81B 9C6C 6C3A 7F46 4173  3F5E E9C6 473D 4E97 DAD1`](https://keys.openpgp.org/vks/v1/by-fingerprint/F81B9C6C6C3A7F4641733F5EE9C6473D4E97DAD1)

## License

Copyright (c) 2023 "dEajL3kA" &lt;Cumpoing79@web.de&gt;  
This work has been released under the MIT license. See [LICENSE.txt](LICENSE.txt) for details!
