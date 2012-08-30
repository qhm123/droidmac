//
//  Phone.h
//  droidmac
//
//  Created by qhm123 on 12-8-30.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Phone : NSObject

@property (retain) NSString *device;
@property (retain) NSString *time;
@property (retain) NSString *model;
@property (retain) NSString *cpuAbi;
@property (retain) NSString *cpuAbi2;
@property (retain) NSString *identifier;
@property (retain) NSString *sdk;
@property (retain) NSString *manufacturer;
@property (retain) NSString *display;

+ (Phone *) fromJson:(NSString *)json;

@end
