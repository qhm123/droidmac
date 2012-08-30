//
//  Phone.m
//  droidmac
//
//  Created by qhm123 on 12-8-30.
//  Copyright (c) 2012年 qhm123. All rights reserved.
//

#import "Phone.h"
#import "JSONKit.h"

@implementation Phone

@synthesize device;
@synthesize time;
@synthesize model;
@synthesize cpuAbi;
@synthesize cpuAbi2;
@synthesize identifier;
@synthesize sdk;
@synthesize manufacturer;
@synthesize display;

+ (Phone *) fromJson:(NSString *)json
{
    Phone *phone = [Phone alloc];
    NSDictionary *data = [json objectFromJSONString];
    [phone setModel:[data valueForKey:@"MODEL"]];
    [phone setDevice:[data valueForKey:@"DEVICE"]];
    [phone setTime:[data valueForKey:@"TIME"]];
    [phone setCpuAbi:[data valueForKey:@"CPU_ABI"]];
    [phone setCpuAbi2:[data valueForKey:@"CPU_ABI2"]];
    [phone setIdentifier:[data valueForKey:@"ID"]];
    [phone setSdk:[data valueForKey:@"SDK_INT"]];
    [phone setManufacturer:[data valueForKey:@"MANUFACTURER"]];
    [phone setDisplay:[data valueForKey:@"DISPLAY"]];
    
    return phone;
}

@end
