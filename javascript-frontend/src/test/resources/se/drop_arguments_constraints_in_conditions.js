function main(cond) {

  var x = null;
  if(cond) x = -1;
  doSomething();     // PS x=NULL || x=NEG_NUMBER
  if(isDef(x)) {
    doSomething();  // PS x=ANY_VALUE
    x = 1;          // PS x=POS_NUMBER
  } else {
    doSomething();  // PS x=ANY_VALUE
  }

  doSomething();  // PS x=ANY_VALUE || x=POS_NUMBER
  makeLive(x);

}
