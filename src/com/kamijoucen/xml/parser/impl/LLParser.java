package com.kamijoucen.xml.parser.impl;

import com.kamijoucen.utils.CollecUtils;
import com.kamijoucen.validate.Validate;
import com.kamijoucen.xml.ast.*;
import com.kamijoucen.xml.ast.result.*;
import com.kamijoucen.xml.exception.XmlSyntaxException;
import com.kamijoucen.xml.parser.Scanner;
import com.kamijoucen.xml.parser.Parser;
import com.kamijoucen.xml.token.Token;
import com.kamijoucen.xml.token.TokenLocation;
import com.kamijoucen.xml.token.TokenType;

import java.util.List;

public class LLParser implements Parser {

    private Scanner scanner;

    public LLParser(Scanner scanner) {
        Validate.notNull(scanner);
        this.scanner = scanner;
        scanner.getNextToken();
    }

    @Override
    public BaseAst parserTagBlock() {
        TagStartAst blockStart = parserTagStart();
        if (blockStart instanceof SingleTagStartAst) {
            return blockStart;
        }
        TagBlockAst blockAst = new TagBlockAst(blockStart.getTagName());
        blockAst.setAttrs(blockStart.getAttrs());
        while (scanner.getToken().getTokenType() != TokenType.TAG_END_START) {
            switch (scanner.getToken().getTokenType()) {
                case IDENTIFIER:
                    TextResult text = parserChildText();
                    blockAst.addText(text);
                    break;
                case TAG_START:
                    BaseAst cb = parserTagBlock();
                    blockAst.addChild(cb);
                    break;
                default:
                    throw new XmlSyntaxException("标签的子节点是未知标识");
            }
        }
        TagEndStartAst blockEnd = parserTagEndStart();
        if (!blockEnd.getTagName().equals(blockStart.getTagName())) {
            throw new XmlSyntaxException(blockEnd.getTokenLocation() + "处应该出现</" + blockStart.getTagName() + ">");
        }
        return blockAst;
    }


    public TagEndStartAst parserTagEndStart() {
        if (scanner.getToken().getTokenType() != TokenType.TAG_END_START) {
            throw new XmlSyntaxException(scanner.getToken().getTokenLocation() + "处需要一个'</'");
        }
        TokenLocation startLocation = scanner.getToken().getTokenLocation();
        Token tag = scanner.getNextToken();
        if (tag.getTokenType() != TokenType.IDENTIFIER) {
            throw new XmlSyntaxException(tag.getTokenLocation() + "处需要一个标签名字");
        }
        if (scanner.getNextToken().getTokenType() != TokenType.TAG_END) {
            throw new XmlSyntaxException(scanner.getToken().getTokenLocation() + "处需要一个标签结束符");
        }
        scanner.getNextToken();
        return new TagEndStartAst(tag.getStrVal(), startLocation);
    }

    @Override
    public TagStartAst parserTagStart() {
        if (scanner.getToken().getTokenType() != TokenType.TAG_START) {
            throw new XmlSyntaxException(scanner.getToken().getTokenLocation() + "应该是一个标签起始符");
        }
        Token tag = scanner.getNextToken();
        if (tag.getTokenType() != TokenType.IDENTIFIER) {
            throw new XmlSyntaxException(tag.getTokenLocation() + "处需要一个标签名字");
        }
        scanner.getNextToken();
        List<AttrResult> attrs = CollecUtils.list();
        while (scanner.getToken().getTokenType() != TokenType.TAG_END
                && scanner.getToken().getTokenType() != TokenType.SINGLE_TAG_END
                && scanner.getToken().getTokenType() != TokenType.END_OF_FILE) {
            attrs.add(parserAttr());
        }

        TokenType end = scanner.getToken().getTokenType();
        scanner.getNextToken();
        if (end == TokenType.TAG_END) {
            return new TagStartAst(tag.getStrVal(), attrs);
        } else if (end == TokenType.SINGLE_TAG_END) {
            return new SingleTagStartAst(tag.getStrVal(), attrs);
        } else {
            throw new XmlSyntaxException(scanner.getToken().getTokenLocation() + "处需要一个标签结束符");
        }
    }

    @Override
    public AttrResult parserAttr() {
        Token key = scanner.getToken();
        Token op = scanner.getNextToken();
        if (op.getTokenType() == TokenType.OPERATE) {
            Token val = scanner.getNextToken();
            if (val.getTokenType() == TokenType.STRING) {
                scanner.getNextToken();
                return new AttrResult(key.getStrVal(), val.getStrVal());
            } else {
                throw new XmlSyntaxException(key.getTokenLocation() + "属性没有找到属性值");
            }
        } else if (op.getTokenType() == TokenType.IDENTIFIER
                || op.getTokenType() == TokenType.TAG_END
                || op.getTokenType() == TokenType.SINGLE_TAG_END) {
            return new AttrResult(key.getStrVal(), "");
        } else {
            throw new XmlSyntaxException(key.getTokenLocation() + "属性后存在未识别的标识符");
        }
    }

    @Override
    public TextResult parserChildText() {
        Token token = scanner.getToken();
        scanner.getNextToken();
        return new TextResult(token.getStrVal());
    }



}
