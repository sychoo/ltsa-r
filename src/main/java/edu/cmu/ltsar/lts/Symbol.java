package edu.cmu.ltsar.lts;

import java.util.*;
import java.awt.Color;

public class Symbol {

	public int kind;
	public int startPos;  //start offset of token in input
  public int endPos = -1;    //end offset of token in input

    private String string;  // holds identifiers as well as string literals
    private int longValue;
    private Object any;     // add additional information


public Symbol () {this.kind = UNKNOWN_TYPE;};

public Symbol (Symbol copy) {
  this.kind = copy.kind;
	this.startPos = copy.startPos;
	this.endPos = copy.endPos;
	this.string = copy.string;
	this.longValue = copy.longValue;
	this.any = copy.any;
}

public Symbol (Symbol copy, String name)  {
	this(copy);
	this.string = name;
}


public Symbol (int kind) {
	this.kind = kind;
	this.startPos = -1;
	this.string = null;
	this.longValue = 0;
}

public Symbol (int kind, String s) {
    this.kind = kind;
	this.startPos = -1;
	this.string = s;
	this.longValue = 0;
}

public Symbol (int kind, int v) {
    this.kind = kind;
	this.startPos = -1;
	this.string = null;
	this.longValue = v;
}

public void setString(String s) {string = s;}
public void setValue(int val) {longValue = val;}
public int  intValue() {return longValue;}
public void setAny(Object o) {any = o;}
public Object getAny() {return any;}
// _______________________________________________________________________________________
// isScalarType

public boolean isScalarType () {
	switch (kind) {
	case	BOOLEAN_TYPE:
	case	DOUBLE_TYPE:
	case	INT_TYPE:
	case	STRING_TYPE:
		return true;

	default:
		return false;
	}
}

/*--------------------------------------------------------*/

private Color commentColor = new Color(102, 153, 153);
private Color upperColor = new Color(0, 0, 160);

public Color getColor() {
  if (kind>0 && kind<=INIT) 
    return Color.blue;
  else if (kind==COMMENT)
    return commentColor;
  else if (kind == INT_VALUE || kind == STRING_VALUE)
    return Color.red;
  else if (kind == UPPERIDENT)
  	return upperColor;
  else
    return Color.black;
}

// _______________________________________________________________________________________
// TOSTRING

public String toString () {
	switch (kind) {

	// _______________________________________________________________________________________
	// Keywords
    case	CONSTANT:				return "const";
    case    PROPERTY:               return "property";
    case    RANGE:                  return "range";
	case	IF:				        return "if";
	case	THEN:					return "then";
	case    ELSE:                   return "else";
	case    FORALL:                 return "forall";
	case    WHEN:                   return "when";
	case    SET:                    return "set";
	case    PROGRESS:               return "progress";
	case    MENU:                   return "menu";
	case    ANIMATION:              return "animation";
	case    ACTIONS:                return "actions";
	case    CONTROLS:               return "controls";
	case    DETERMINISTIC:          return "determinstic";
	case    MINIMAL:                return "minimal";
  case    COMPOSE:                return "compose";
  case    TARGET:                 return "target";
  case    IMPORT:                 return "import";
  case    UNTIL:                  return "U";
  case    ASSERT:                 return "assert";
  case    PREDICATE:              return "fluent";
  case    NEXTTIME:               return "X";
  case    EXISTS:                   return "exists";
  case    RIGID:                   return "rigid";
  case    CONSTRAINT:				return "constraint";
  case    LTLPROPERTY:              return "ltl_property";
  case    SAFE:                    return "safe";
  case    INIT:                   return "initially";

	case	BOOLEAN_TYPE:	        return "boolean";
	case	DOUBLE_TYPE:	        return "double";
	case	INT_TYPE:			    return "int";
	case	STRING_TYPE:	        return "string";
	case	UNKNOWN_TYPE:	        return "unknown";

	// _______________________________________________________________________________________

	case	UPPERIDENT: 			return string;
	case	IDENTIFIER: 			return string;
	case    LABELCONST:             return string;
	case	INT_VALUE:				return longValue + "";
	case	STRING_VALUE:			return string;

	// _______________________________________________________________________________________
	// Expression symbols

	case 	UNARY_MINUS:				    return "-";
	case 	UNARY_PLUS:					    return "+";
	case 	PLUS:							return "+";
	case 	MINUS:							return "-";
	case 	STAR:							return "*";
	case 	DIVIDE:							return "/";
	case 	MODULUS:						return "%";
	case 	CIRCUMFLEX:					    return "^";
	case 	SINE:							return "~";
	case 	QUESTION:						return "?";
	case 	COLON:							return ":";
	case    COLON_COLON:                    return "::";
	case 	COMMA:							return ",";
	case 	OR:								return "||";
	case 	BITWISE_OR:					    return "|";
	case 	AND:							return "&&";
	case 	BITWISE_AND:				    return "&";
	case 	NOT_EQUAL:					    return "!=";
	case 	PLING:							   return "!";
	case 	LESS_THAN_EQUAL:		   return "<=";
	case 	LESS_THAN:					   return "<";
	case 	SHIFT_LEFT:					   return "<<";
	case 	GREATER_THAN_EQUAL: 	   return ">=";
	case 	GREATER_THAN:				   return ">";
	case 	SHIFT_RIGHT:				   return ">>";
	case 	EQUALS:							 return "==";
	case 	LROUND:							 return "(";
	case 	RROUND:							 return ")";
	case  QUOTE:                   return "'";
	case  HASH:                    return "#";
	case  EVENTUALLY:              return "<>";
	case  ALWAYS:                  return "[]";
	case  EQUIVALENT:              return "<->";

	// _______________________________________________________________________________________
	// Others

	case 	LCURLY:				return "{";
	case 	RCURLY:				return "}";
	case 	LSQUARE:			return "[";
	case 	RSQUARE:			return "]";
	case 	BECOMES:			return "=";
	case 	SEMICOLON:		    return ";";
	case 	DOT:	    		return ".";
	case 	DOT_DOT:			return "..";
	case 	AT:					return "@";
	case    ARROW:              return "->";
	case    BACKSLASH:          return "\\";
	// _______________________________________________________________________________________
	// Special

	case 	EOFSYM:				return "EOF";
	default:
		return "ERROR";
	}
}

// _______________________________________________________________________________________
// Keywords
public static final int CONSTANT = 1;
public static final int PROPERTY = 2;
public static final int RANGE = 3;
public static final int IF   = 4;
public static final int THEN =  5;
public static final int ELSE = 6;
public static final int FORALL = 7;
public static final int WHEN = 8;
public static final int SET = 9;
public static final int PROGRESS = 10;
public static final int MENU = 11;
public static final int ANIMATION = 12;
public static final int ACTIONS   = 13;
public static final int CONTROLS  = 14;
public static final int DETERMINISTIC = 15;
public static final int MINIMAL = 16;
public static final int COMPOSE = 17;
public static final int TARGET  = 18;
public static final int IMPORT  = 19;
public static final int UNTIL   = 20;
public static final int ASSERT   = 21;
public static final int PREDICATE   = 22;
public static final int NEXTTIME   = 23;
public static final int EXISTS      = 24;
public static final int RIGID     = 25;
public static final int CONSTRAINT = 26;
public static final int LTLPROPERTY = 27;
public static final int SAFE = 28;
public static final int INIT       = 29;



public static final int BOOLEAN_TYPE = 102;
public static final int DOUBLE_TYPE = 103;
public static final int INT_TYPE = 104;
public static final int STRING_TYPE = 105;
public static final int UNKNOWN_TYPE = 106;

public static final int	UPPERIDENT = 123;
public static final int	IDENTIFIER = 124;


// _______________________________________________________________________________________
// Expression symbols

public static final int	UNARY_MINUS = 33;					// unary -
public static final int	UNARY_PLUS = 34;					// unary +
public static final int	PLUS = 35;								// +
public static final int	MINUS = 36;								// -
public static final int	STAR = 37;								// *
public static final int	DIVIDE = 38;							// /
public static final int	MODULUS = 39;							// %
public static final int	CIRCUMFLEX = 40;					// ^
public static final int	SINE = 41;								// ~
public static final int	QUESTION = 42;						// ?
public static final int	COLON = 43;								// :
public static final int	COMMA = 44;								// ,
public static final int	OR = 45;									// ||
public static final int	BITWISE_OR = 46; 					// |
public static final int	AND = 47;									// &&
public static final int	BITWISE_AND = 48;					// &
public static final int	NOT_EQUAL = 49;						// !=
public static final int	PLING = 50;								// !
public static final int	LESS_THAN_EQUAL = 51;			// <=
public static final int	LESS_THAN = 52;						// <
public static final int	SHIFT_LEFT = 53;					// <<
public static final int	GREATER_THAN_EQUAL = 54;	// >=
public static final int	GREATER_THAN = 55;				// >
public static final int	SHIFT_RIGHT = 56;					// >>
public static final int	EQUALS = 57;							// ==
public static final int	LROUND = 58;							// (
public static final int	RROUND = 59;							// )

// _______________________________________________________________________________________
// Others

public static final int	LCURLY = 60;							// {
public static final int	RCURLY = 61;							// }
public static final int	LSQUARE = 62;							// [
public static final int	RSQUARE = 63;							// ]
public static final int	BECOMES = 64;							// =
public static final int	SEMICOLON = 65;						// ;
public static final int	DOT = 66;									// .
public static final int	DOT_DOT = 67;							// ..
public static final int	AT = 68;									// @
public static final int	ARROW = 69;						// ->
public static final int  BACKSLASH = 70;                 // \
public static final int  COLON_COLON = 71;               // ::
public static final int  QUOTE = 72;                      //'
public static final int  HASH  = 73;                   //#

//________________________________________________________________________________________
// Linear Temporal Logic Symbols
public static final int EVENTUALLY   = 74;               //<>
public static final int ALWAYS       = 75;               //[]
public static final int EQUIVALENT   = 76;               //<->
public static final int WEAKUNTIL    = 77;				  //W
// _______________________________________________________________________________________
// Special
public static final int LABELCONST = 98;
public static final int	EOFSYM = 99;
public static final int COMMENT = 100;

// _______________________________________________________________________________________
// _______________________________________________________________________________________

public static final int	INT_VALUE = 125;
public static final int	DOUBLE_VALUE = 126;
public static final int	STRING_VALUE = 127;


}