package com.morefireworks.naming;

/** Whether a name is the compact display form or the lossless structural form. */
public enum NameMode {
    /** Short, human friendly summary. May collide for different parameter sets. */
    SHORT,
    /** Full structural name. Distinguishes every parameter set the mod can produce. */
    FULL
}
