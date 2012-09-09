//
//  NaviDelegate.m
//  droidmac
//
//  Created by qhm123 on 12-9-8.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import "NaviDelegate.h"

@implementation NaviDelegate


- (NSView *)outlineView:(NSOutlineView *)outlineView viewForTableColumn:(NSTableColumn *)tableColumn item:(id)item {
    NSLog(@"outlineView: %@", item);
    //return [[NSButton alloc] init];
    
    return [[NSTextField alloc] init];
}

-(void)outlineViewSelectionDidChange:(NSNotification *)notification
{
    NSLog(@"selection changed");
}


@end
