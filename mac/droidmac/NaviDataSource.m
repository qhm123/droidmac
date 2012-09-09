//
//  NaviDataSource.m
//  droidmac
//
//  Created by qhm123 on 12-9-8.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import "NaviDataSource.h"

@implementation NaviDataSource

- (NaviDataSource*) init {
    self = [super init];
    
    items = [[NSArray alloc] initWithObjects:@"设备", @"应用", @"联系人", @"短信", @"媒体", nil];
    
    return self;
}

- (NSInteger)outlineView:(NSOutlineView *)outlineView numberOfChildrenOfItem:(id)item {
    return (item == nil) ? items.count : [item count];
}

- (BOOL)outlineView:(NSOutlineView *)outlineView isItemExpandable:(id)item {
    return NO;
}

- (id)outlineView:(NSOutlineView *)outlineView child:(NSInteger)index ofItem:(id)item {
    
    return (item == nil) ? [items objectAtIndex:index] : [item objectAtIndex:index];
}

- (id)outlineView:(NSOutlineView *)outlineView objectValueForTableColumn:(NSTableColumn *)tableColumn byItem:(id)item {
    return item;
}

@end
