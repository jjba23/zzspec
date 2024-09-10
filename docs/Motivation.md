
# zzspec - why ?

(zzspec stands for *black box test*)

Testing at a high level  means freedom of implementation and refactoring. This is something all engineers love 💘

Read below for some of the motivation behind writing this library.


# zzspec is good at BDD

This stimulates functional and behaviour driven testing.

Testing a system becomes simpler and we can cover many more "real" edge cases.


# Unit testing still has its place

Unit testing is an invaluable tool that should also be used in parallel to zzspec and other high level tests.

It is very easy to use, useful and gives also good information about a system.

Ideally, your "core domain" should be fully tested, and your "business logic" should be encoded in more pure code, ideally as data.


# Dealing with dependencies

When we start using external dependencies in a system like databases, caches, external APIs, etc.

You have 3 choices:

-   create and use mocks
-   create and use stubs (dummy implementations)
-   use real dependencies


# Mocks are inherently evil

Mocks are generally painful to write, read, debug and maintain.

They should be avoided when possible, we should use real implementations for most tests to a system.

testcontainers is a great library for this purpose, and we use it extensively.


# Java baggage

Lots of developers coming from a traditional enterprisy JDK environment know what we mean.

This tendency of doing one class per file, and creating tests for every single class and every single method along with extensive Mockito ideology.


# The cure

No marrying the test structure to code structure. No testing every individual class when not needed.

Ensure we test functionalities, almost never test how the code is written / implemented (this gives flexibility and freedom to refactor).

Many black box tests. Attempt to test all “user paths” and possible interactions with the system, good and bad, low load high load, etc.

Many unit tests where it makes sense. Preferably auto-generated test cases, with a set of inputs.

# Pros ?

Easier and simpler tests of the entire system, tests have lower complexity

Easy to cover 100% of a “user flow” or a “data flow”

Low chance of false positives (partly thanks to avoiding mocks too)

This allows for a good test-driven development approach, and more confidence in product.

Testers require less technical knowledge, programming or IT skills and do not need to learn all nitty gritty implementation details of the system

More loose coupling from the code means more freedom of implementation + refactor

