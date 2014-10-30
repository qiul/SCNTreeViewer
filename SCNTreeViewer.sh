#!/bin/sh
STANFORD_CORENLP_HOME=${HOME}/Shared/Java/lib/stanford-corenlp-full-2014-01-04
java -mx3g -cp "${STANFORD_CORENLP_HOME}/*:./classes" edu.nus.comp.nlp.stanford.UtilParser "$@" 


